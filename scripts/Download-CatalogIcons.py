"""Download the free icon originals used by the requested catalog additions."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import json
import urllib.request

root = Path(__file__).resolve().parents[1] / 'images' / 'catalog'
root.mkdir(exist_ok=True)
icons = ['filter', 'wind', 'gas-pump', 'bolt', 'car-battery', 'gears', 'gear',
         'temperature-half', 'droplet', 'circle-dot', 'oil-can', 'car',
         'screwdriver-wrench', 'rotate', 'magnifying-glass', 'laptop-medical',
         'fan', 'lightbulb', 'shield-halved']

def fetch(icon):
    url = f'https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/svgs/solid/{icon}.svg'
    with urllib.request.urlopen(url, timeout=20) as response:
        data = response.read()
    if b'<svg' not in data:
        raise ValueError(f'Invalid SVG response: {icon}')
    (root / f'{icon}.svg').write_bytes(data)
    return {'icon': icon, 'source': url}

if __name__ == '__main__':
    with ThreadPoolExecutor(max_workers=4) as pool:
        sources = list(pool.map(fetch, icons))
    with urllib.request.urlopen('https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/LICENSE.txt', timeout=20) as response:
        (root / 'LICENSE.txt').write_bytes(response.read())
    (root / 'SOURCES.json').write_text(json.dumps(sources, indent=2), encoding='utf-8')
    (root / 'README.md').write_text(
        'Catalog icons: Font Awesome Free by Fonticons, Inc. https://fontawesome.com\n\n'
        'Source: https://github.com/FortAwesome/Font-Awesome/tree/6.x/svgs/solid\n'
        'License: CC BY 4.0, https://creativecommons.org/licenses/by/4.0/\n'
        'See LICENSE.txt and SOURCES.json. PNG adaptations recolor and position the SVG paths '
        'on a charcoal background for the existing Customer theme. These are illustrative '
        'category icons, not product photographs.\n', encoding='utf-8')
    print(f'Downloaded {len(sources)} icons; license/source notices saved in images/catalog')
