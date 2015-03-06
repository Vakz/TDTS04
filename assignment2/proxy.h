#include <string>
#include <vector>

class Proxy
{
public:
  // Returns socket descriptor
  int listen_for_incoming(int port_nr);

private:
  int setup_listening(int port_nr);
  int setup_client(std::string const& url);
  
  void handle_incoming_request(int incoming_socketfd);
  void send_to_client(int socketfd, std::string const& data);
  void pass_through_data(int client_socketfd, int external_socketfd);
  void filter_or_pass_through(int client_socketfd, int external_socketfd);
  std::string recieve_data(int socketfd, size_t maximum_data = 15000);
  std::string get_host(std::string const& request);
  void make_redirect(int client_socketfd, bool bad_url);

  /**
   * Returns true if no bad words are found
   * Returns false if url contains bad words, or if request is malformed
   */
  bool allowed_url(std::string const& client_request);
  void modify_connection_type(std::string& request);
  void modify_request(std::string& request);
  void remove_encoding_header(std::string& request);
  bool is_text_content(std::string const& request);

  std::vector<std::string> kBadWords
  {
    "spongebob",
    "norrköping",
    "britney spears",
    "paris hilton"
  };
};
