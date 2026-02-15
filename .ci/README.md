# Qodana Baseline

This directory contains the Qodana baseline SARIF file used for static code analysis.

## Baseline File

`qodana-baseline.sarif.json` - This file contains the baseline of known issues. Qodana will only report issues that are not in this baseline file.

## Generating a New Baseline

To regenerate the baseline (useful when you want to accept current issues as the new baseline):

1. Temporarily remove or rename the existing baseline file
2. Modify `qodana.yaml` to comment out the `baseline` configuration
3. Run `./gradlew qodanaScan`
4. Copy the generated SARIF file from `build/reports/qodana/result-allProblems.sarif.json` to `.ci/qodana-baseline.sarif.json`
5. Restore the `baseline` configuration in `qodana.yaml`
6. Commit the new baseline file

## Zero Tolerance Policy

The project is configured with `failThreshold: 0` which means **any new issue** that is not in the baseline will cause the build to fail. This ensures code quality does not degrade over time.
