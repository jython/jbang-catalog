import json, os, sys

os.system("jbang info tools %s > jbang-deps.json"%(sys.argv[1]))
d = json.load(open("jbang-deps.json"))
os.system("jbang toolbox@maveniverse versions " + ",".join(d["dependencies"]))