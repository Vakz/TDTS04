#include "filter.h"
#include <algorithm>
#include <iostream>
#include <regex>
#include <string>
#include <map>
#include <boost/locale.hpp>
#include <boost/algorithm/string/case_conv.hpp>


bool Filter::contains_bad_strings(std::string const& str,
                                 std::vector<std::string> const& bad_words)
{
  std::string line{boost::locale::to_lower(str)};
  for(std::string const& word : bad_words)
  {
    std::string pattern{"(.|\\s)*" + word + "(.|\\s)*"};
    if (std::regex_match(line, std::regex(pattern)))
    {
      return true;
    }
  }
  return false;
}


void Filter::encode_characters_url(std::string& word)
{
  encode_decode_characters(word, kUrlCharacters, true);
}

void Filter::encode_characters_text(std::string& word)
{
  encode_decode_characters(word, kTextCharacters, true);
}

void Filter::decode_characters_url(std::string& word)
{
  encode_decode_characters(word, kUrlCharacters, false);
}

void Filter::decode_characters_text(std::string& word)
{
  encode_decode_characters(word, kTextCharacters, false);
}

void Filter::encode_decode_characters(std::string word, std::map<std::string, std::string> const& char_map, bool encode)
{
  using c_map = std::map<std::string, std::string>;
  
  for (c_map::const_iterator it{char_map.cbegin()}; it != char_map.end(); ++it)
  {
    std::string c{encode ? it->first : it->second};
    size_t pos{word.find(c)};
    if (pos != std::string::npos)
    {
      word.replace(pos, c.length(), it->first);
    }
  }
}

const std::map<std::string, std::string> Filter::kUrlCharacters
{
  std::make_pair("Ö", "%C3%96"),
  std::make_pair("Ä", "%C3%84"),
  std::make_pair("Å", "%C3%85"),
  std::make_pair(" ", "%20"),
  std::make_pair("ö", "%C3%B6"),
  std::make_pair("ä", "%C3%A4"),
  std::make_pair("å", "%C3%A5")
};

const std::map<std::string, std::string> Filter::kTextCharacters{};
