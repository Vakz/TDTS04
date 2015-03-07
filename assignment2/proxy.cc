#include "proxy.h"
#include <iostream>
#include <stdexcept>
#include <sys/socket.h>
#include <netdb.h>
#include <cstring>
#include <unistd.h>
#include <sstream>
#include <regex>
#include <vector>
#include <boost/locale.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/algorithm/string/trim_all.hpp>
#include "filter.h"

int Proxy::listen_for_incoming(int port_nr)
{
  int socketfd{setup_listening(port_nr)};
  while(true)
  {
    struct sockaddr_storage their_addr;
    socklen_t addr_size = sizeof their_addr;
    // Accept incoming connection, then fork it
    int new_fd = accept(socketfd,
                        reinterpret_cast<struct sockaddr *>(&their_addr),
                        &addr_size);
    if (new_fd == -1)
    {
      std::cout << "listen error" << std::endl ;
    }
    else
    {
      std::cout << "Connection accepted. Using new socketfd : "
                <<  new_fd << std::endl;
    }
    
    if (!fork())
    {
      // Child process
      close(socketfd);
      handle_incoming_request(new_fd);
      close(new_fd);
      exit(0);
    }
    else
    {
      close(new_fd);
    }
  }
  return 0;
}

int Proxy::setup_listening(int port_nr)
{
  struct addrinfo host_info;
  struct addrinfo* host_info_list;
  memset(&host_info, 0, sizeof host_info);

  // Set up structs
  host_info.ai_flags = AI_PASSIVE;
  host_info.ai_family = AF_UNSPEC;
  host_info.ai_socktype = SOCK_STREAM;
  std::cout << "Setting up addr structs.." << std::endl;
  std::string port = std::to_string(port_nr);
  int status = getaddrinfo(NULL, port.c_str(), &host_info, &host_info_list);

  // Creat the socket
  std::cout << "Creating socket.." << std::endl;
  int socketfd = socket(host_info_list->ai_family, host_info_list->ai_socktype,
                    host_info_list->ai_protocol);

  if (socketfd == -1)
  {
    std::cerr << "Unable to create socket.." << std::endl;
    exit(2);
  }

  // Bind socket
  std::cout << "Binding socket.." << std::endl;
  int yes = 1;
  status = setsockopt(socketfd, SOL_SOCKET,SO_REUSEADDR, &yes, sizeof(int));
  status = bind(socketfd, host_info_list->ai_addr, host_info_list->ai_addrlen);
  freeaddrinfo(host_info_list);

  if (status == -1)
  {
    std::cerr << "Unable to bind socket.." << std::endl;
    exit(3);
  }
  
  // Start listening
  std::cout << "Setting up listening.." << std::endl;
  status = listen(socketfd, 5);

  if (status == -1)
  {
    std::cerr << "Unable to start listening.." << std::endl;
    exit(4);
  }
  return socketfd;
}

int Proxy::setup_client(std::string const& url)
{
  int status;
  struct addrinfo host_info;
  struct addrinfo* host_info_list;

  memset(&host_info, 0, sizeof host_info);
  host_info.ai_family = AF_UNSPEC;
  host_info.ai_socktype = SOCK_STREAM;
  status = getaddrinfo(url.c_str(), "80", &host_info, &host_info_list);

  if (status != 0)
  {
    std::cerr << "Issue when getting address on external request to " << url << std::endl;
    return -1;
  }

  int socketfd{socket(host_info_list->ai_family, host_info_list->ai_socktype,
                      host_info_list->ai_protocol)};
  if (socketfd == -1)
  {
    std::cerr << "Issue when creating socket on external request to " << url << std::endl;
    return -1;
  }

  status = connect(socketfd, host_info_list->ai_addr, host_info_list->ai_addrlen);
  if (status == -1){
    std::cerr << "Unable to connect to " << url << std::endl;
    return -1;
  }
  return socketfd;
  
}

std::string Proxy::recieve_data(int socketfd, size_t maximum_data)
{
  ssize_t bytes;
  size_t total_recieved{0};
  std::string recieved_data = "";
  int maximum_per_iteration = 1400;
  char* iteration_buffer = new char[maximum_per_iteration];
  do{
    bytes = recv(socketfd, iteration_buffer, maximum_per_iteration, 0);
    recieved_data.append(iteration_buffer, bytes);
    total_recieved += bytes;
    if (bytes == 0) return "";
  } while (bytes == maximum_per_iteration
           // Avoid overflowing buffer
           && total_recieved < (maximum_data - maximum_per_iteration));
    //} while (bytes > 0);
  
  delete[] iteration_buffer;
  return recieved_data;
}

void Proxy::pass_through_data(int client_socketfd, int external_socketfd)
{
  std::string buffer;
  do
  {
    // Do transfers in chunks of 100000 bytes
    buffer = recieve_data(external_socketfd, 100000);
    if (buffer.empty()) break;
    send_to_client(client_socketfd, buffer);
  } while (buffer.length() != buffer.max_size());
}

void Proxy::handle_incoming_request(int incoming_socketfd)
{
  

  std::string recieved_data{recieve_data(incoming_socketfd)};
  if(!allowed_url(recieved_data))
  {
    make_redirect(incoming_socketfd, true);
  }
  else
  {
    modify_request(recieved_data);

    // Set up a new connection to request target
    std::string host{get_host(recieved_data)};
    int external_socket{setup_client(get_host(recieved_data))};
    // Pass on original request
    send_to_client(external_socket, recieved_data);
    // Pass on any data request target gives to client
    filter_or_pass_through(incoming_socketfd, external_socket);
    close(external_socket);
    
  }
}

void Proxy::filter_or_pass_through(int client_socketfd, int external_socketfd)
{
  std::string recieved_data{""};

  // Get Header data, should not be more than 1000 bytes
  recieved_data += recieve_data(external_socketfd, 1000);
  if ( is_text_content(recieved_data))
  {
    // If text content, take in entire site, to filter it,
    // then either redirect or send page to client
    
    std::string iteration{""};   
    do
    {
      iteration = recieve_data(external_socketfd, 3000);
      recieved_data += iteration;
    } while (!iteration.empty());
    std::string copy{recieved_data};
    Filter::decode_characters_text(copy);
    if (Filter::contains_bad_strings(copy, kBadWords))
    {
      make_redirect(client_socketfd, false);
    }
    else
    {
      send_to_client(client_socketfd, recieved_data);
    }
  }
  else
  {
    send_to_client(client_socketfd, recieved_data);
    pass_through_data(client_socketfd, external_socketfd);
  }
}

std::string Proxy::get_host(std::string const& request)
{
  std::stringstream ss{request};
  std::string line{""};
  std::string host;
  // Assume header is formatted as "Host: www.example.com[:80]"
  while(getline(ss, line))
  {
    if (std::regex_match(line, std::regex("^Host: (.|\\s)*")))
    {
      std::vector<std::string> strings;
      boost::split(strings, line, boost::is_any_of(":"));
      host = strings[1];
      boost::trim_all(host);
      return host;
    }
  }
  throw std::invalid_argument("Request does not contain Host header");
}

void Proxy::make_redirect(int client_socketfd, bool bad_url)
{
  std::string host{"www.ida.liu.se"};
  // Build request string
  std::string request{"GET /~TDTS04/labs/2011/ass2/error"};
  request += bad_url ? "1.html" : "2.html";
  request += " HTTP/1.1\nhost: www.ida.liu.se\n\n";
  
  int socketfd{setup_client(host)};
  // Make request to external server, in this case the
  // redirection site
  send_to_client(socketfd, request);

  // Take reply
  std::string recieved_data{recieve_data(socketfd)};
  
  send_to_client(client_socketfd, recieved_data);
  close(socketfd);
}

void Proxy::send_to_client(int socketfd, std::string const& data)
{
  int data_length = int(data.length()); // To avoid issues with signed and unsigned comparision
  ssize_t bytes;
  ssize_t sent = 0;
  int buffer_size{1400};
  do
  {
    int data_to_send = (data_length - sent > buffer_size) ? buffer_size : data_length - sent;
    bytes = send(socketfd, data.c_str() + sent, data_to_send, 0);
    sent += bytes;
  } while (sent < data_length);
  
}

bool Proxy::is_text_content(std::string const& request)
{
  std::stringstream ss{request};
  std::string line{""};

  // Verified regex
  while (getline(ss, line))
  {
    if (std::regex_match(line, std::regex(
                           "^Content-Type: text\\/(.|\\s)*",
                           std::regex_constants::icase)))
    {
      // Text content
      return true;
    }
  }
  // Binary content type, or no content-type header found (assume binary)
  return false;
}

void Proxy::modify_connection_type(std::string& request)
{
  std::regex pattern{".*Connection: (keep-alive).*",
      std::regex_constants::icase};
  request =
    std::regex_replace(request,
                       pattern,
                       "Connection: close");
}

void Proxy::modify_request(std::string& request)
{
  remove_host_from_status(request);
  modify_connection_type(request);
  remove_encoding_header(request);
}

void Proxy::remove_host_from_status(std::string& request)
{
  std::regex pattern{"(GET )http://[\\w.]+[\\w]+(.*)"};
  request = std::regex_replace(request, pattern, "$1$2");
}

void Proxy::remove_encoding_header(std::string& request)
{
  request = std::regex_replace(request, std::regex("Accept-Encoding: .*\r\n", std::regex_constants::icase), "");
}

bool Proxy::allowed_url(std::string const& client_request)
{
  std::stringstream ss{client_request};
  std::string line{""};
  
  while(getline(ss, line))
  {
    if (std::regex_match(line, std::regex("^GET (.|\\s)*")))
    {
      
      Filter::decode_characters_url(line);                                               
      return !Filter::contains_bad_strings(line, kBadWords);
    }
  }
  // No GET-header, assume request is not for a page document, thus no URL
  return true;
}
