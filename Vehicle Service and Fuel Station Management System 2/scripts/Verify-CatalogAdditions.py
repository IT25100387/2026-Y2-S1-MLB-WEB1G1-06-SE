"""Read-only verification of the requested catalog rows and role feeds."""
from pathlib import Path
import http.cookiejar
import hashlib
import json
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
BASE = 'http://127.0.0.1:8080'
REPORTS = ROOT / 'data' / 'catalog-tools'

def session(username, password):
    client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    client.open(urllib.request.Request(BASE + '/api/auth/login',
        data=urllib.parse.urlencode({'username': username, 'password': password}).encode(),
        headers={'Content-Type': 'application/x-www-form-urlencoded'}), timeout=20).close()
    def get(path):
        with client.open(BASE + path, timeout=20) as response:
            return json.load(response)
    return client, get

def verify():
    manifest = json.loads((ROOT / 'images/catalog/catalog-additions.json').read_text(encoding='utf-8'))
    snapshot = REPORTS / 'catalog-before-additions.json'
    before = json.loads(snapshot.read_text(encoding='utf-8')) if snapshot.exists() else None
    if before:
        before = {kind: value[kind] if isinstance(value, dict) else value for kind, value in before.items()}
    client, get = session('manager', 'manager@123')
    now = {'parts': get('/api/inventory/parts')['parts'], 'services': get('/api/services')['services']}
    if before:
        for kind in ('parts', 'services'):
            by_id = {row['id']: row for row in now[kind]}
            assert all(row == by_id[row['id']] for row in before[kind]), 'Existing records changed'
    rows = []
    for kind, name in [('parts', 'partName'), ('services', 'name')]:
        for item in manifest[kind]:
            matches = [row for row in now[kind] if row[name] == item[name]]
            assert len(matches) == 1, 'Duplicate or missing catalog entry'
            row = matches[0]
            for field, value in item.items():
                if field != 'catalogIcon': assert row.get(field) == value, (item[name], field)
            assert row['imageUrl'].startswith('/uploads/catalog/')
            with client.open(BASE + row['imageUrl'], timeout=20) as response:
                content = response.read()
                assert response.headers.get_content_type() == 'image/png'
            expected = ROOT / 'images/catalog' / (item['catalogIcon'] + '.png')
            assert hashlib.sha256(content).digest() == hashlib.sha256(expected.read_bytes()).digest()
            rows.append({'kind': kind, 'id': row['id'], 'name': item[name], 'imageUrl': row['imageUrl']})
    feeds = []
    for role, user, password, endpoints in [
        ('Customer', 'customer1', 'customer1@123', [('/api/v1/customer/store', 'parts', 'partName'), ('/api/v1/customer/services', 'services', 'name')]),
        ('Cashier', 'cashier', 'cashier@123', [('/api/pos/products', 'parts', 'partName'), ('/api/pos/catalog', 'services', 'name')])]:
        _, get_role = session(user, password)
        for endpoint, kind, name in endpoints:
            data = get_role(endpoint)
            assert isinstance(data, list)
            assert len(data) == len(now[kind])
            assert all(any(row[name] == item[name] and row.get('imageUrl') for row in data) for item in manifest[kind])
            feeds.append({'role': role, 'endpoint': endpoint, 'count': len(data)})
    report = {'newParts': 30, 'newServices': 10, 'existingRecordsUnchanged': True if before else None,
              'allNewImagesVerified': True, 'feeds': feeds, 'rows': rows}
    REPORTS.mkdir(parents=True, exist_ok=True)
    (REPORTS / 'catalog-additions-verification.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in report.items() if k != 'rows'}, indent=2))

if __name__ == '__main__':
    verify()
