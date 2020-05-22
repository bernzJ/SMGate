# SMGate

Simple SMS gateway for sending built on top of nanohttpd (bundled as service, should run on pretty much any sdk).

# Note

This have no built in receive function.
All messages sent will be saved in your default sms app.
If you need more complex usage of the message, IE: one message for X phone. Feel free to edit the logic here: [WebServer.js](https://github.com/bernzJ/SMGate/blob/master/app/src/main/java/com/benz/smgate/WebServer.java#L81).

# Usage

The server expect a json object with the following params:
```javascript
{
  "phones": ["321981321", "3219089021"] // Array of string
  "message": "hello!" // String
}
```

Python example:
```python
import json
import urllib.request

data = {"phones": ["03928109381"], "message": "test"}
params = json.dumps(data).encode('utf8')

with urllib.request.urlopen(urllib.request.Request('http://192.168.0.102:8080/', data=params, headers={'content-type': 'application/json'})) as response:
   html = response.read()
   print(html)
```
