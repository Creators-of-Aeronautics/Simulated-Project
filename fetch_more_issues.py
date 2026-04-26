import urllib.request, ssl, json, sys

proxy = urllib.request.ProxyHandler({'https': 'http://127.0.0.1:7890', 'http': 'http://127.0.0.1:7890'})
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
opener = urllib.request.build_opener(proxy, urllib.request.HTTPSHandler(context=ctx))

# More issues to investigate
issue_nums = [
    691,  # Anvil drops when assembled
    590,  # Some blocks like buttons drop and duplicate on contraption assembly
    315,  # Redstone components dupe when assembled
    329,  # Redstone components on vehicles duplicated
    549,  # Redstone links dupe upon contraption assembly
    687,  # Create + Aeronautics Mods Malicious Item-Duplication Exploit
    684,  # Massive Duplication Bug
    697,  # Item is duplicated when I assemble Refined Storage Disk Drive
    617,  # Schematicannon requests white hot air envelopes when using coloured ones
    154,  # Stationary Rope Connector connections do not save when pasted from a schematic
    143,  # Respawn point is not reset when contraption bed is destroyed
    140,  # When Bed gets rotated, respawning crashes the game
    294,  # Save corruption sleeping on bed attached to Simulated craft
    670,  # Velocity Sensor display value ignores Towards/Away configuration
    300,  # Gimbal sensor not outputting redstone signal
    585,  # Scaffolding incorrectly reads 0kpg when actually weights 1 kpg
    196,  # Air current on simulated contraption does not respect fan_transparent tag
    555,  # Fan washing is weirdly conditional for when it washes on airships
    331,  # Fan wind on a simulated contraption is going through blocks
    664,  # Can't crystallize levitite blend in server
    569,  # Random block ticks was null Error when attempting to rotate Swivel Bearing
    672,  # Deployer with shears on rope connector crashes the game
    594,  # Deployer Placement direction change with rotation on sublevels
    488,  # Deployers can cause holes in simulated structures
    408,  # Weighted Ejector teleports items to any distance when target is converted
    357,  # Propellers continue spinning when source broken
    631,  # Portable generator stress is consumed for no apparent reason
    112,  # Iron Handles negate fall damage while shift-grabbing
    557,  # Steering wheels fail to register clicks while in motion
    116,  # Item tooltips do not show the complete mod name
]

for num in issue_nums:
    url = f'https://api.github.com/repos/Creators-of-Aeronautics/Simulated-Project/issues/{num}'
    req = urllib.request.Request(url)
    req.add_header('User-Agent', 'Python')
    try:
        resp = opener.open(req, timeout=30)
        data = json.loads(resp.read().decode())
        labels = ', '.join([l['name'] for l in data.get('labels', [])])
        body = (data.get('body') or '')[:1500]
        with open('more_issues.jsonl', 'a', encoding='utf-8') as f:
            json.dump({'number': data['number'], 'title': data['title'], 'labels': labels, 'body': body}, f, ensure_ascii=False)
            f.write('\n')
        print(f'Fetched #{data["number"]}: {data["title"]}')
    except Exception as e:
        print(f'Error fetching #{num}: {e}')

print('Done!')
