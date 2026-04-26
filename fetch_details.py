import urllib.request, ssl, json, sys

proxy = urllib.request.ProxyHandler({'https': 'http://127.0.0.1:7890', 'http': 'http://127.0.0.1:7890'})
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
opener = urllib.request.build_opener(proxy, urllib.request.HTTPSHandler(context=ctx))

# Issues to fetch details for - prioritized by fixability
issue_ids = [
    670,  # Velocity Sensor display value ignores "Towards/Away" configuration
    692,  # Single block propellers don't produce any sound
    693,  # Unable to long press Q key to discard continuous items
    691,  # Anvil drops when assembled
    660,  # Crash when pasting a schematic containing a Navigation Table with a map from another world
    569,  # "Random block ticks was null" Error when attempting to rotate Swivel Bearing
    664,  # Can't crystallize levitite blend in server
    698,  # Fixes a null pointer crash in MapNavigationTarget
    568,  # Bug with case letter in "ru_Ru" file
    537,  # Localization file failed to load correctly
    533,  # translation files bug
    526,  # Incorrect i18n
    522,  # Translation is not effective
    130,  # Typewriter Small UI Translation missing
    617,  # Schematicannon requests white hot air envelopes when using coloured ones
    300,  # Gimbal sensor not outputting redstone signal
    143,  # Respawn point is not reset when contraption bed is destroyed
    112,  # Iron Handles negate fall damage while shift-grabbing
    154,  # Stationary Rope Connector connections do not save when pasted from a schematic
    196,  # Air current on simulated contraption does not respect fan_transparent tag
    357,  # Propellers continue spinning when source broken
    585,  # Scaffolding incorrectly reads 0kpg when actually weights 1 kpg
    672,  # Deployer with shears on rope connector crashes the game
    56,   # name plate cannot input chinese
    116,  # Item tooltips do not show the complete mod name
    265,  # Creative Physics Staff and Honey Glue LMB Function do not work if Attack/Destroy key bound to non-mouse
    217,  # NullPointerException when assembling physics entity with Leaves Be Gone mod
    335,  # Water does not update next to assembling contraptions
    223,  # Using physics assembler to physicalize blocks on water's edge will not trigger updates to water
    408,  # Weighted Ejector teleports items to any distance when target is converted to simulated contraption
]

results = []
for iid in issue_ids:
    url = f'https://api.github.com/repos/Creators-of-Aeronautics/Simulated-Project/issues/{iid}'
    req = urllib.request.Request(url)
    req.add_header('User-Agent', 'Python')
    try:
        resp = opener.open(req, timeout=30)
        data = json.loads(resp.read().decode())
        body = data.get('body', '') or ''
        # Truncate body to first 3000 chars
        body = body[:3000]
        results.append({
            'number': data['number'],
            'title': data['title'],
            'labels': [l['name'] for l in data.get('labels', [])],
            'body': body,
            'state': data['state']
        })
        print(f'Fetched #{iid}: {data["title"]}')
    except Exception as e:
        print(f'Error fetching #{iid}: {e}')

with open('issue_details.json', 'w', encoding='utf-8') as f:
    json.dump(results, f, ensure_ascii=False, indent=2)

print(f'\nTotal fetched: {len(results)} issues')
