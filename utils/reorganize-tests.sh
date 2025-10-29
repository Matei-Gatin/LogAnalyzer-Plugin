# Create correct test directory structure
mkdir -p src/test/java/com/github/mateigatin/loganalyzerplugin/analyzer
mkdir -p src/test/java/com/github/mateigatin/loganalyzerplugin/model
mkdir -p src/test/java/com/github/mateigatin/loganalyzerplugin/parser

# Move analyzer tests
for file in PerformanceAnalyzerTest SecurityAnalyzerTest StatusCodeAnalyzerTest TopEndpointsAnalyzerTest TotalRequestAnalyzerTest TrafficByHourAnalyzerTest; do
  if [ -f "src/test/java/com/github/mateigatin/loganalyzerplugin/${file}.java" ]; then
    mv "src/test/java/com/github/mateigatin/loganalyzerplugin/${file}.java" \
       "src/test/java/com/github/mateigatin/loganalyzerplugin/analyzer/"
  fi
done

# Move model tests
for file in ApacheLogEntryTest HttpStatusTest; do
  if [ -f "src/test/java/com/github/mateigatin/loganalyzerplugin/${file}.java" ]; then
    mv "src/test/java/com/github/mateigatin/loganalyzerplugin/${file}.java" \
       "src/test/java/com/github/mateigatin/loganalyzerplugin/model/"
  fi
done

# Move parser test
if [ -f "src/test/java/com/github/mateigatin/loganalyzerplugin/ApacheLogParserTest.java" ]; then
  mv "src/test/java/com/github/mateigatin/loganalyzerplugin/ApacheLogParserTest.java" \
     "src/test/java/com/github/mateigatin/loganalyzerplugin/parser/"
fi

# Update package declarations in analyzer tests
find src/test/java/com/github/mateigatin/loganalyzerplugin/analyzer -name "*.java" -type f -exec sed -i '1c\package com.github.mateigatin.loganalyzerplugin.analyzer;' {} +

# Update package declarations in model tests
find src/test/java/com/github/mateigatin/loganalyzerplugin/model -name "*.java" -type f -exec sed -i '1c\package com.github.mateigatin.loganalyzerplugin.model;' {} +

# Update package declarations in parser tests
find src/test/java/com/github/mateigatin/loganalyzerplugin/parser -name "*.java" -type f -exec sed -i '1c\package com.github.mateigatin.loganalyzerplugin.parser;' {} +

echo "Test files reorganized successfully!"