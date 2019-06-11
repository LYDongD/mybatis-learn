/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Clinton Begin
 */
public class GenericTokenParser {

  private final String openToken;
  private final String closeToken;
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }


  public String parseByRegex(String text) {
    Matcher m = Pattern.compile("\\$\\{(.*?)\\}").matcher(text);
    while (m.find()) {
      text = text.replace(m.group(), handler.handleToken(m.group(1)));
    }
    return text;
  }

  /**
   * 占位符替换算法，例如${variable}替换成具体成具体属性值
   * 1 考虑转义字符的情况
   * 2 使用属性字典进行替换
   * 3 字符串 -> 字符数组 -> 循环查找 open/close token -> 替换 -> 拼接组合
   * @param text 变量
   * @return
   */
  public String parse(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    // search open token
    int start = text.indexOf(openToken);
    if (start == -1) {
      return text;
    }
    char[] src = text.toCharArray();
    int offset = 0;

    //use string builder to build final string to return
    final StringBuilder builder = new StringBuilder();

    //use expression to build variable string : ${varibale}
    StringBuilder expression = null;

    //update offset after appending by offset and length [offsert, start/end]
    while (start > -1) {
      if (start > 0 && src[start - 1] == '\\') {
        // this open token is escaped. remove the backslash and continue. 转义字符，更新查找的起始偏移量 offset, 去掉转义字符后拼接openToekn
        builder.append(src, offset, start - offset - 1).append(openToken);
        offset = start + openToken.length();
      } else {
        // found open token. let's search close token.
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        builder.append(src, offset, start - offset);
        offset = start + openToken.length();
        //serch close token
        int end = text.indexOf(closeToken, offset);
        while (end > -1) {
          if (end > offset && src[end - 1] == '\\') {
            // this close token is escaped. remove the backslash and continue.
            expression.append(src, offset, end - offset - 1).append(closeToken);
            offset = end + closeToken.length();
            end = text.indexOf(closeToken, offset);
          } else {
            expression.append(src, offset, end - offset);
            offset = end + closeToken.length();
            break;
          }
        }
        if (end == -1) {
          // close token was not found.
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          builder.append(handler.handleToken(expression.toString()));
          offset = end + closeToken.length();
        }
      }
      //find all the ${} by openToken
      start = text.indexOf(openToken, offset);
    }
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    return builder.toString();
  }
}
