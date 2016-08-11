## Integration tests
Integration tests are run in phase integration-test. To run them use mvn verify.

Tests are using by default url http://pnc-host/pnc-rest/rest to connect to PNC (you can define IP for pnc-host in your hosts file).

Alternatively you can set the value by setting system property PNC_URL (eg. -DPNC_URL=http://my-pnc-host/pnc-rest/rest)

## Disabling integration tests for PNC
Integration tests for PNC can be disabled by setting -Ddisable-pnc-it

## Dependencies
To compile causeway you need to compile:
 * https://github.com/Commonjava/propulsor
   * https://github.com/Commonjava/jhttpc
 * https://github.com/project-ncl/pnc
 * https://github.com/release-engineering/kojiji
