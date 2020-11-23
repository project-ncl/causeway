## Integration tests
test
Integration tests are run in phase integration-test. To run them use mvn verify.

Tests are using by default url http://pnc-host/pnc-rest/rest to connect to PNC (you can define IP for pnc-host in your hosts file).

Alternatively you can set the value by setting system property PNC_URL (eg. -DPNC_URL=http://my-pnc-host/pnc-rest/rest)

## Disabling integration tests for PNC
Integration tests for PNC can be disabled by setting -Ddisable-pnc-it

## Dependencies
To compile causeway you need to compile:
 * https://github.com/project-ncl/pnc
 * https://github.com/release-engineering/kojiji

## Metrics support

PNC tracks metrics of JVM and its internals via Dropwizard Metrics. The metrics can currently be reported to a Graphite server by specifying as system property or environment variables those properties:
- metrics\_graphite\_server (mandatory)
- metrics\_graphite\_port (mandatory)
- metrics\_graphite\_prefix (mandatory)
- metrics\_graphite\_interval (optional)

If the `metrics_graphite_interval` variable (interval specified in seconds) is not specified, we'll use the default value of 60 seconds to report data to Graphite.

The graphite reporter is configured to report rates per second and durations in terms of milliseconds.
