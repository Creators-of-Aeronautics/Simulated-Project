import json

with open('issues.json', encoding='utf-8') as f:
    data = json.load(f)

targets = [670, 693, 692, 660, 680, 568, 655, 524, 521, 569, 310, 154, 644, 643, 585, 579, 508, 357, 300, 283, 223, 335, 590, 315, 329, 691, 617, 652, 594, 664]

for issue in data:
    if issue['number'] in targets:
        body = issue.get('body', '') or ''
        print(f'=== #{issue["number"]} {issue["title"]} ===')
        print(body[:2000])
        print()
