// "Collapse loop with stream 'anyMatch()'" "true-preview"

import java.util.List;

public class Main {
  public void testAssignment(List<String> data) {
    String[] found = {"no"};
      if (data.stream().map(String::trim).anyMatch(trimmed -> !trimmed.isEmpty())) {
          found[0] = "yes";
      }
  }
}