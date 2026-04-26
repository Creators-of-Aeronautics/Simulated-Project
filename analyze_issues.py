import json, sys, os
sys.stdout.reconfigure(encoding='utf-8')

data = json.load(open('issues.json', 'r', encoding='utf-8'))
print(f'Total issues: {len(data)}')

# Filter for bug-type issues
bugs = [i for i in data if any(l['name'] in ['type: bug', 'type: crash'] for l in i.get('labels', []))]
print(f'Bug/crash issues: {len(bugs)}')
print()
for i in bugs:
    labels = ', '.join([l['name'] for l in i.get('labels', [])])
    print(f'#{i["number"]}: {i["title"]} [{labels}]')
