Compiles with -lboost_locale

Depends on libboost-locale-dev

High-level algorithm:
-------------------------------
1. Have client connect to proxy. For a child process to handle the request.
2. Proxy checks URL for unallowed words
3. - END - If URL is NOT OK, respond with 301 and redirect
4. If URL is OK, forward request to external server
5. Take response from external server.
6. Check content-type. If any of the following, filter:
         
         - text/html
         - application/x-javascript
         - application/javascript
         - application/json
         - text/xml
         - application/xml
         - text/css
           OR POSSIBLY:
         - text/*
         - application/*
7. If disallowed words found, respond with 301 and redirect
8. Else, return response as given by external server