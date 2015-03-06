#ifndef _FILTER_H_
#define _FILTER_H_

#include <vector>
#include <string>
#include <utility>
#include <map>

class Filter
{
public:
  static bool contains_bad_strings(std::string const& str, std::vector<std::string> const& bad_words);
  static void encode_characters_text(std::string& word);
  static void encode_characters_url(std::string& word);
  static void decode_characters_text(std::string& word);
  static void decode_characters_url(std::string& word);

private:
  static void encode_decode_characters(std::string word, std::map<std::string, std::string> const& char_map, bool encode);
  
  static const std::map<std::string, std::string> kUrlCharacters;
  static const std::map<std::string, std::string> kTextCharacters;
};

#endif
