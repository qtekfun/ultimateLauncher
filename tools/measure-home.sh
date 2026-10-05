#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 UltimateLauncher contributors
# SPDX-License-Identifier: GPL-3.0-or-later
# Mide la geometría del inicio del launcher actual en el dispositivo y la compara con la referencia OPPO.
# Uso: tools/measure-home.sh <serie>   (guarda captura y volcado en private-measurements/, ignorado por git)
set -euo pipefail
cd "$(dirname "$0")/.."
S="${1:-}"; A="adb -s $S"; P=private-measurements
$A shell input keyevent KEYCODE_HOME; sleep 3
$A exec-out screencap -p > $P/measure.png
$A shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; $A pull /sdcard/u.xml $P/measure.xml >/dev/null; $A shell rm /sdcard/u.xml
~/work/venv/bin/python - <<PY
import re, numpy as np, cv2
x=open('$P/measure.xml').read()
cells=[(re.search(r'content-desc="([^"]*)"',n).group(1), list(map(int,re.findall(r'\d+',re.search(r'bounds="([^"]*)"',n).group(1))))) for n in re.findall(r'<node[^>]*>',x) if re.search(r'content-desc="[^"]+"',n) and 'Inicio' not in n]
rid=lambda r:[list(map(int,re.findall(r'\d+',re.search(r'bounds="([^"]*)"',n).group(1)))) for n in re.findall(r'<node[^>]*>',x) if 'resource-id="com.qtekfun.ultimatelauncher:id/'+r+'"' in n]
print('hotseat',rid('hotseat'),'indicador',rid('page_indicator'))
for c in cells: print(' ',c[0][:16],c[1])
def bbox(path,y0,y1,x0,x1,thr=235):
    im=cv2.imread(path)[y0:y1,x0:x1]; ys,xs=np.where(im.min(axis=2)>=thr)
    return (xs.min()+x0,ys.min()+y0,xs.max()+x0,ys.max()+y0)
o=bbox('$P/home.png',2700,3100,20,320); u=bbox('$P/measure.png',2700,3100,20,320)
print('icono dock  OPPO',o,o[2]-o[0]+1,'px | Ultimate',u,u[2]-u[0]+1,'px')
PY
