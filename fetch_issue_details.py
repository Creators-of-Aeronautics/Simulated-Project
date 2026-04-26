import urllib.request, ssl, json, sys

proxy = urllib.request.ProxyHandler({'https': 'http://127.0.0.1:7890', 'http': 'http://127.0.0.1:7890'})
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
opener = urllib.request.build_opener(proxy, urllib.request.HTTPSHandler(context=ctx))

# Fetch details for specific issues that look fixable
issue_nums = [568, 537, 533, 526, 522, 56, 130, 664, 660, 217, 691, 693, 692, 670, 585, 143, 265, 116, 154, 617]

for num in issue_nums:
    url = f'https://api.github.com/repos/Creators-of-Aeronautics/Simulated-Project/issues/{num}'
    req = urllib.request.Request(url)
    req.add_header('User-Agent', 'Python')
    try:
        resp = opener.open(req, timeout=30)
        data = json.loads(resp.read().decode())
        labels = ', '.join([l['name'] for l in data.get('labels', [])])
        body = (data.get('body') or '')[:2000]
        print(f'===== #{data["number"]}: {data["title"]} [{labels}] =====')
        print(body)
        print()
    except Exception as e:
        print(f'Error fetching #{num}: {e}')
        print()
