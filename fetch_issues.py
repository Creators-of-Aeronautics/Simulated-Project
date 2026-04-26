import urllib.request, ssl, json, sys

proxy = urllib.request.ProxyHandler({'https': 'http://127.0.0.1:7890', 'http': 'http://127.0.0.1:7890'})
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE
opener = urllib.request.build_opener(proxy, urllib.request.HTTPSHandler(context=ctx))

all_issues = []
for page in range(1, 5):
    url = f'https://api.github.com/repos/Creators-of-Aeronautics/Simulated-Project/issues?state=open&per_page=100&page={page}'
    req = urllib.request.Request(url)
    req.add_header('User-Agent', 'Python')
    try:
        resp = opener.open(req, timeout=30)
        data = json.loads(resp.read().decode())
        if not data:
            break
        all_issues.extend(data)
        print(f'Page {page}: got {len(data)} issues')
    except Exception as e:
        print(f'Page {page} error: {e}')
        break

with open('issues.json', 'w', encoding='utf-8') as f:
    json.dump(all_issues, f, ensure_ascii=False, indent=2)

print(f'Total: {len(all_issues)} issues saved to issues.json')
for i in all_issues:
    labels = ', '.join([l['name'] for l in i.get('labels', [])])
    print(f'#{i["number"]}: {i["title"]} [{labels}]')
