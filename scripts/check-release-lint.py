from pathlib import Path
import xml.etree.ElementTree as E
root = E.parse(Path("app/build/reports/lint-results-release.xml")).getroot()
errors = [i for i in root.findall("issue") if i.get("severity") in ("Error", "Fatal")]
unexpected = [i for i in errors if not (i.get("id") == "ExpiredTargetSdkVersion" and any(n.get("file", "").endswith("app/build.gradle.kts") for n in i.findall("location")))]
print("Release lint: inherited target-SDK issue:", len(errors) - len(unexpected), "; other errors:", len(unexpected))
for issue in unexpected:
    print(issue.get("id"), issue.get("message"))
if unexpected:
    raise SystemExit(1)
