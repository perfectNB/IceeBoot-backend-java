#!/usr/bin/env python3
"""Agent 模块的轻量提交检查；--staged 检查 Git 索引中的实际提交内容。"""
from pathlib import Path
import re
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
staged = "--staged" in sys.argv
paths = subprocess.check_output(
    ["git", "ls-files", "--cached"] if staged else ["git", "ls-files", "--cached", "--others", "--exclude-standard"],
    cwd=root, text=True,
).splitlines()
errors = []
for name in sorted(set(paths)):
    if not name.startswith("src/main/java/") or not name.endswith(".java"):
        continue
    if staged:
        result = subprocess.run(["git", "show", f":{name}"], cwd=root, capture_output=True, text=True)
        if result.returncode:
            continue
        source = result.stdout
    else:
        path = root / name
        if not path.exists():
            continue
        source = path.read_text()
    if re.search(r"package\s+com\.iceeboot\.agent(?:\.|;)", source):
        errors.append(f"{name}: Agent 代码必须遵守 framework/config/common 包结构")
    if "/agent/" not in name:
        continue
    if re.search(r"throw\s+new\s+(?!IceeBootException\b)[\w.]+\s*\(", source):
        errors.append(f"{name}: 主动抛出的异常必须使用 IceeBootException")
    if re.search(r'throw\s+new\s+IceeBootException\s*\(\s*"', source):
        errors.append(f"{name}: 异常消息必须引用 ResponseMessage.Agent 常量")
    if "/controller/" in name:
        annotations = re.findall(r"@RequirePermission\(([^)]*)\)", source)
        if not annotations or any(not re.search(r"dynamic\s*=\s*true", item) for item in annotations):
            errors.append(f"{name}: Controller 必须使用动态权限校验")
    for pattern in (r"\bsk-[A-Za-z0-9]{24,}", r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"):
        if re.search(pattern, source):
            errors.append(f"{name}: 检测到疑似真实凭据，禁止提交")
if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Agent 包结构、异常、权限和凭据检查通过")
