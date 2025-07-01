import json, os, sys

os.system("jbang info tools %s > jbang-deps.json"%(sys.argv[1]))
file = open("jbang-deps.json")
d = json.load(file)
file.close()
os.unlink("jbang-deps.json")
os.system("jbang toolbox@maveniverse versions " + ",".join(d["dependencies"]))
