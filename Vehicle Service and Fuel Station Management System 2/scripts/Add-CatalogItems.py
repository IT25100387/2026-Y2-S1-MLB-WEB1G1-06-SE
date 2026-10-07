"""One-time, repeat-safe importer for the user's 30 parts and 10 services.

Uses existing authenticated CRUD/photo APIs. Never resets records or creates offers.
"""
from pathlib import Path
import http.cookiejar
import json
import os
import urllib.parse
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:8080'
REPORTS = ROOT / 'data' / 'catalog-tools'

def importer():
    jar = http.cookiejar.CookieJar()
    client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    credentials = urllib.parse.urlencode({'username': os.environ.get('FUELCORE_IMPORT_USER', 'manager'),
                                         'password': os.environ.get('FUELCORE_IMPORT_PASSWORD', 'manager@123')}).encode()
    client.open(urllib.request.Request(BASE + '/api/auth/login', data=credentials,
                headers={'Content-Type': 'application/x-www-form-urlencoded'}), timeout=20).close()
    def get(path):
        with client.open(BASE + path, timeout=20) as response:
            return json.load(response)
    if get('/api/auth/me').get('role') not in ('ROLE_ADMIN', 'ROLE_MANAGER'):
        raise RuntimeError('Sign in with an account allowed to manage inventory and services')
    before = {'parts': get('/api/inventory/parts')['parts'], 'services': get('/api/services')['services'],
              'suppliers': get('/api/inventory/suppliers')['suppliers']}
    existing_offers = {kind: get('/api/offers/' + kind)['offers'] for kind in ('spare-parts', 'services')}
    REPORTS.mkdir(parents=True, exist_ok=True)
    snapshot = REPORTS / 'catalog-before-additions.json'
    if not snapshot.exists():
        snapshot.write_text(json.dumps(before, indent=2), encoding='utf-8')
    catalog = json.loads((ROOT / 'images/catalog/catalog-additions.json').read_text(encoding='utf-8'))
    supplier = next((s['supplierName'] for s in before['suppliers'] if s.get('status', 'Active') == 'Active'), None)
    added = {'parts': [], 'services': []}
    skipped = {'parts': [], 'services': []}
    for kind, key, field, endpoint in [('parts', 'part', 'partName', '/api/inventory/parts/add'),
                                      ('services', 'service', 'name', '/api/services/add')]:
        existing = {item[field].casefold() for item in get('/api/inventory/parts' if kind == 'parts' else '/api/services')[kind]}
        for original in catalog[kind]:
            if original[field].casefold() in existing:
                skipped[kind].append(original[field]); continue
            item = {k: v for k, v in original.items() if k != 'catalogIcon'}
            if kind == 'parts': item['supplier'] = supplier
            boundary = 'FuelCoreCatalog' + uuid.uuid4().hex
            body = bytearray(f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\nContent-Type: application/json\r\n\r\n'.encode())
            body += json.dumps(item).encode() + b'\r\n'
            photo = ROOT / 'images/catalog' / (original['catalogIcon'] + '.png')
            if photo.is_file():
                body += f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="{photo.name}"\r\nContent-Type: image/png\r\n\r\n'.encode()
                body += photo.read_bytes() + b'\r\n'
            body += f'--{boundary}--\r\n'.encode()
            request = urllib.request.Request(BASE + endpoint, data=bytes(body),
                      headers={'Content-Type': f'multipart/form-data; boundary={boundary}'})
            with client.open(request, timeout=30) as response:
                result = json.load(response)
            if not result.get('success'): raise RuntimeError(f'Failed to add {original[field]}')
            existing.add(original[field].casefold()); added[kind].append(original[field])
    after = {'parts': get('/api/inventory/parts')['parts'], 'services': get('/api/services')['services']}
    for kind in ('parts', 'services'):
        by_id = {row['id']: row for row in after[kind]}
        for row in before[kind]:
            if row != by_id.get(row['id']): raise RuntimeError(f'An existing {kind} record changed: {row["id"]}')
    for kind, rows in existing_offers.items():
        if rows != get('/api/offers/' + kind)['offers']: raise RuntimeError('An existing offer changed')
    report = {'added': added, 'skipped': skipped, 'counts': {k: len(v) for k, v in after.items()},
              'existingRecordsUnchanged': True, 'offersUnchanged': True, 'createdOffers': False}
    (REPORTS / 'catalog-additions-result.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    (REPORTS / 'catalog-after-additions.json').write_text(json.dumps(after, indent=2), encoding='utf-8')
    print(json.dumps({'addedParts': len(added['parts']), 'addedServices': len(added['services']),
                      'totalParts': len(after['parts']), 'totalServices': len(after['services']),
                      'existingRecordsUnchanged': True}, indent=2))

if __name__ == '__main__':
    importer()
