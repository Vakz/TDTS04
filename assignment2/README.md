Depends on libboost-locale-dev

Compiles with command g++ filter.cc proxy.cc main.cc -lboost_locale

High-level algorithm:
-------------------------------
1. Have client connect to proxy. For a child process to handle the request.
2. Proxy checks URL for unallowed words
3. - END - If URL is NOT OK, respond with 301 and redirect
4. If URL is OK, change connection type to Close and disallow gzip-encoding,
   then forward request to external server
5. Take response from external server.
6. Check content-type. If content-type: text/*, filter
7. If disallowed words found, respond with 301 and redirect
8. Else, return response as given by external server

User manual
--------------------------------
System requirements:
	Compiled with gcc 4.9
	Depends on libboost1.54-dev.
	May on some installs require libboost-locale-dev.
Compile with command:
	g++ main.cc proxy.cc filter.cc -lboost_locale

The proxy does not have any configuration alternatives other than changing the port on which it operates, unless you change the code itself. To use the proxy you first need to run a compiled version of it in the terminal, with the argument which is the portnumber. This number needs to be in the range 1025 – 65535.

Requirement fulfillment

Requirement 2: The proxy handles simple HTTP GET interactions between the client and a server. The request from the client is first handled in Proxy::listen_for_incoming, then handled in Proxy::handle_incoming_request, which determines if the URL is allowed or not. If it is, the clients request is forwarded to the request target in Proxy::send_to_client (with a somewhat misleading name).

Requirement 3: URL Filtering is done in Proxy::allowed_url, using Filter::contains_bad_strings.
Requirement 6: There is no browser-specific code. The proxy works for all tested browsers.

Requirement 7: This is done immediately on start, as the port is entered as an argument to the program.

Requirement 8: Proxy::is_text_content checks whether a file should be filtered or not, by checking the content-type.

Tested for
-------------------------------
The proxy works for most tested sites.

The sites we have tested are:

svt.se

reddit.com

dailymotion.com (tested streaming some videos)

wikipedia.org (correct redirects when appropriate)

norrkoping.se (correctly redirects)

The proxy filters all url requests and web content that contains forbidden words as listed in the code.

If a forbidden word is found it will redirect the user to an appropriate error page. But the proxy does not filter https requests.
