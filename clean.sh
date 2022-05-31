#!/usr/bin/env bash

set -eux

TSV_FILE=$1
CSV_FILE=transactions.csv

cat "${TSV_FILE}" | tr "\\t" "," > "${CSV_FILE}"

cat "${CSV_FILE}" \
  | tr "\\t" "," \
  | grep '^,[0-9]' \
  | sed 's/^,//g' \
  | sed 's/,[^,]$//g' \
  | sed 's/1,668/1668/g' \
  | sed 's/ - Action Direct//g' \
  | sed 's/ - Cdn//g' \
  | sed 's/ - Cdn//g' \
  | sed 's/ - /,/g' \
  | sed 's/\.000//g' \
  | sed 's/Alphabet,Class A/KEEP-GOOGL/g' \
  | sed 's/Alphabet,Class C/KEEP-GOOG/g' \
  | sed 's/Amazon/KEEP-AMZN/g' \
  | sed 's/Apple/KEEP-AAPL/g' \
  | sed 's/Berkshire Hathaway - Class B/KEEP-BRK.B/g' \
  | sed 's/Cameco Corp/KEEP-TSE:CCO/g' \
  | sed 's/iShares Core S&P 500 Index ETF (CAD-Hedged)/KEEP-TSE:XSP/g' \
  | sed 's/iShares Diversified Monthly Income ETF/KEEP-TSE:XTR/g' \
  | sed 's/ISHARES S&P\/TSX 60 INDEX FUND/KEEP-TSE:XIU/g' \
  | sed 's/ISHARES S&P\/TSX SMALLCAP INDEX FUND/KEEP-TSE:XCS/g' \
  | sed 's/iShares Core S&P Total US Stock Market ETF/KEEP-ITOT/g' \
  | grep "KEEP" \
  | sed 's/KEEP-//g' \
  | awk -F "," '{ print $2, $3, $5, $1, $4, $6 }' \
  | sort




