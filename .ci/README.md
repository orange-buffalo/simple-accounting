# Qodana Baseline

This directory contains the Qodana baseline SARIF file used for static code analysis.

## Baseline File

`qodana-baseline.sarif.json` - This file contains the baseline of known issues. Qodana will only report issues that are not in this baseline file.

## Generating a New Baseline

To regenerate the baseline (useful when you want to accept current issues as the new baseline):

```bash
./gradlew qodanaRebaseline
```

This will:
1. Run Qodana scan
2. Copy the results to `.ci/qodana-baseline.sarif.json`
3. Remind you to commit the new baseline file

## Zero Tolerance Policy

The project is configured with `failThreshold: 0` which means **any new issue** that is not in the baseline will cause the build to fail. This ensures code quality does not degrade over time.
