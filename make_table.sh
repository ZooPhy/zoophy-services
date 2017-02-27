# the first argument is the loganalyser output
# the second argument is the cleaned file to write to
filename=$1
output=$2
	
# print out the entire contents of the file
cat "${filename}" |
	
# delete everything beginning with "coefficientsTimesIndicators" through the end of the file
sed -e '/coefficientsTimesIndicators1/,$d' |
	
# delete everything up through the "burnIn" line
# results in just the "cleaned" data needed
sed '1,/burnIn/d' > "$output"