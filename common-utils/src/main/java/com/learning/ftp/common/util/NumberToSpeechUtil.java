package com.learning.ftp.common.util;

import java.util.Stack;
import org.springframework.util.StringUtils;

public class NumberToSpeechUtil {
  private static final String SINGLE = "đồng";
  private static final String DOZEN = "mươi";
  private static final String HUNDRED = "trăm";
  private static final String THOUSAND = "nghìn";
  private static final String MILLION = "triệu";
  private static final String BILLION = "tỷ";

  private static final String[] words =
      new String[] {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
  private static final String[] basicUnits = new String[] {"", DOZEN, HUNDRED};

  public static String toSpeech(long money) {
    if (money == 0) {
      return String.format("không %s", SINGLE);
    }

    return StringUtils.capitalize(cleanMoneyWords(convertNumberToText(money)));
  }

  public static String toSpeechWithoutEndingInDong(long money) {
    return toSpeech(money).replaceAll(" đồng$", "");
  }

  private static String convertNumberToText(long money) {
    StringBuilder result = new StringBuilder();
    Stack<String> stack = new Stack<>();
    int idx = 0;
    String unit = getUnit(idx), newUnit;
    stack.push(unit);
    while (money > 0) {
      newUnit = getUnit(idx);
      if (!unit.equals(newUnit)) {
        if (BILLION.equals(newUnit)) {
          stack.push(BILLION);
          stack.push(toSpeech(money).replaceAll(SINGLE, ""));
          break;
        }
        stack.push(newUnit);
        unit = newUnit;
      }
      int d = (int) (money % 10);
      if (d == 0 && idx % 3 == 1) {
        stack.push("lẻ");
      } else {
        stack.push(basicUnits[idx % 3]);
        stack.push(words[d]);
      }
      money = money / 10;
      idx++;
    }
    while (!stack.isEmpty()) {
      result.append(stack.pop()).append(" ");
    }
    return result.toString();
  }

  private static String cleanMoneyWords(String moneyWords) {
    moneyWords =
        moneyWords
            .replaceAll("\\s{2,}", " ")
            .replaceAll("không trăm lẻ không(\\s\\S+)|lẻ không", "")
            .replaceAll("\\s{2,}", " ")
            .trim();
    if (!moneyWords.endsWith(SINGLE)) {
      moneyWords = String.format("%s %s", moneyWords, SINGLE);
    }
    moneyWords =
        moneyWords
            .replaceAll("mươi năm", "mươi lăm")
            .replaceAll("mươi không", "mươi")
            .replaceAll("một mươi", "mười")
            .replaceAll("mươi một", "mươi mốt");
    return moneyWords;
  }

  private static String getUnit(int idx) {
    if (idx < 3) {
      return SINGLE;
    }
    if (idx < 6) {
      return THOUSAND;
    }
    if (idx < 9) {
      return MILLION;
    }
    return BILLION;
  }
}
