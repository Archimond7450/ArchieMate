#!/bin/sh

MINIMAL_REQUIRED_COVERAGE="30.0"
BIG_SEPARATOR="============================================================================="
MINIMAL_REQUIRED_COVERAGE_FULFILLED="YEP"
COVERAGE_MARKDOWN_FILE="coverage.md"

get_percentage() {
    result_code=0
    result=$(grep -E "<text class=\"${1}coverage\"" "$2" | grep -Eo '((\d?\d?\d.?\d?%)|(N/A))' | sed 's/%//g' | uniq)
    if [ "$result" = "N/A" ]; then
        result="100"
    fi
    if [ $(lesser_than "$result" "$MINIMAL_REQUIRED_COVERAGE") = "YEP" ]; then
        result_code=1
    fi
    echo $result
    unset result
    return $result_code
}

lesser_than() {
    awk -v n1="$1" -v n2="$2" 'BEGIN { if (n1 < n2) { print("YEP") } else { print("NOP") } }' /dev/null
}

avg_of_three() {
    awk -v n1="$1" -v n2="$2" -v n3="$3" 'BEGIN { print ((n1 + n2 + n3) / 3) }' /dev/null
}

h1() {
    echo "$BIG_SEPARATOR"
    echo "$1"
}

generate_report() {
    report_output_file=$(mktemp)
    dotnet tool run reportgenerator \
        -reports:$1 \
        -targetdir:$2 \
        -reporttypes:$3 >"$report_output_file" 2>&1
    result=$?
    if [ "$result" -ne "0" ]; then
        cat "$report_output_file" >&2
    fi
    return $result
}

print_markdown_h1() {
    printf "# %s" "$1"
}

print_markdown_h2() {
    printf "## %s" "$1"
}

print_markdown_project_coverage() {
    result_code=0
    project_coverage_svg=./ArchieMate.CoverageReports/$1/badge_combined.svg
    line_percentage="$(get_percentage line $project_coverage_svg)"
    if [ "$?" -ne "0" ]; then
        result_code=1
    fi
    branch_percentage="$(get_percentage branch $project_coverage_svg)"
    if [ "$?" -ne "0" ]; then
        result_code=1
    fi
    method_percentage="$(get_percentage method $project_coverage_svg)"
    if [ "$?" -ne "0" ]; then
        result_code=1
    fi
    average_percentage="$(avg_of_three $line_percentage $branch_percentage $method_percentage)"
    
    echo $(print_markdown_h2 "$1") >>"$2"
    echo "| Line | Branch | Method | Average |" >>"$2"
    echo "| ---- | ------ | ------ | ------: |" >>"$2"
    echo "| $line_percentage | $branch_percentage | $method_percentage | $average_percentage |" >>"$2"
    echo "" >>"$2"
    
    return $result_code
}

h1 "Removing previous coverages and reports"
rm -r ./*/TestResults >/dev/null 2>&1
rm -r ./ArchieMate.Frontend/coverage >/dev/null 2>&1
rm -r ./ArchieMate.FinalCoverageReport >/dev/null 2>&1
rm -r ./ArchieMate.CoverageReports >/dev/null 2>&1

h1 "Restoring tools"
dotnet tool restore >/dev/null 2>&1

for unit_test in *.UnitTests; do
    h1 "Running tests and generating coverages for $unit_test"
    test_output_file="$(mktemp)"
    dotnet test $unit_test --collect:"XPlat Code Coverage" >"$test_output_file" 2>&1
    if [ "$?" -ne "0" ]; then
        echo "ERROR: Tests failed!" >&2
        cat "$test_output_file"
        rm "$test_output_file"
        exit 1
    fi
    rm "$test_output_file"
done

h1 "Running tests and generating coverage for ArchieMate.Frontend"
test_output_file="$(mktemp)"
cd ArchieMate.Frontend
npm run testWithCoverage >"$test_output_file" 2>&1
test_result=$?
cd ..
if [ "$test_result" -ne "0" ]; then
    echo "ERROR: Tests failed!" >&2
    cat "$test_output_file"
    rm "$test_output_file"
    exit 1
fi
rm "$test_output_file"

h1 "Generating badges"
for project in ArchieMate.Frontend *.UnitTests; do
    echo $project
    if [ "$project" = "ArchieMate.Frontend" ]; then
        current_coverage_file=./$project/TestResults/coverage/coverage.cobertura.xml
        mkdir -p ./$project/TestResults
        mv ./$project/coverage ./$project/TestResults/
        mv ./$project/TestResults/coverage/cobertura-coverage.xml $current_coverage_file
    else
        current_coverage_file=./$project/TestResults/*/coverage.cobertura.xml
    fi
    generate_report $current_coverage_file "./ArchieMate.CoverageReports/$project" "Badges"
    if [ "$?" -ne "0" ]; then
        echo "ERROR: Report generator failed to generate badge!" >&2
        exit 1
    fi
done

h1 "Generating final report"
generate_report "./*/TestResults/*/coverage.cobertura.xml" "./ArchieMate.FinalCoverageReport" "Html"
if [ "$?" -ne "0" ]; then
    echo "ERROR: Report generator failed to generate final HTML report!" >&2
    exit 1
fi

h1 "Generating coverage markdown file"
echo $(print_markdown_h1 "ArchieMate code coverage summary") >"$COVERAGE_MARKDOWN_FILE"
for project in ArchieMate.Frontend *.UnitTests; do
    print_markdown_project_coverage "$project" "$COVERAGE_MARKDOWN_FILE" || MINIMAL_REQUIRED_COVERAGE_FULFILLED="NOP"
done
echo $(print_markdown_h2 "Coverage requirement fulfillment") >>"$COVERAGE_MARKDOWN_FILE"
fullfillment_word=""
if [ "$MINIMAL_REQUIRED_COVERAGE_FULFILLED" != "YEP" ]; then
    fullfillment_word=" NOT"
fi
echo "The minimal required coverage of ${MINIMAL_REQUIRED_COVERAGE} percent was${fullfillment_word} fulfilled." >>"$COVERAGE_MARKDOWN_FILE"

h1 "DONE"

[ "$MINIMAL_REQUIRED_COVERAGE_FULFILLED" = "YEP" ]
