#include <iostream>
#include <string>
#include <stdexcept>
#include "proxy.h"
#include <regex>
#include <map>
#include <utility>
#include <boost/locale.hpp>


int main(int argc, char* argv[])
{
  
  using namespace boost::locale;
  
  generator gen;
  std::locale loc=gen("sv_SE.UTF-8");
  std::locale::global(loc);

  if (argc != 2)
  {
    std::cout << "Usage: proxy port(1024 < integer < 65536)" << std::endl;
    exit(1);
  }

  int port = 0;
  
  try
  {
    port = std::stoi(argv[1]);
    if (port <= 1024 || port > 65536)
    {
      throw std::invalid_argument("Invalid port number");
    }
  }
  catch(...)
  {
    std::cout << "Not a valid port number" << std::endl;
    exit(1);
  }

  Proxy p;
  p.listen_for_incoming(port);
  
}
