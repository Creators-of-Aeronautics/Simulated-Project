import json

with open('issues.json', 'r', encoding='utf-8') as f:
    issues = json.load(f)

bugs = [i for i in issues if any(l['name'] in ['type: bug', 'type: crash'] for l in i.get('labels', []))]

with open('bug_issues.txt', 'w', encoding='utf-8') as f:
    f.write(f'Total open issues: {len(issues)}\n')
    f.write(f'Bug/Crash issues: {len(bugs)}\n\n')
    for i in bugs:
        labels = ', '.join([l['name'] for l in i.get('labels', [])])
        f.write(f'#{i["number"]}: {i["title"]} [{labels}]\n')

print('Done - wrote bug_issues.txt')
