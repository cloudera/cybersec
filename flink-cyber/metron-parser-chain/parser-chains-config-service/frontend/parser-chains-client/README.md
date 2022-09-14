# Parser Chaining

This project was initialized from a skeleton version of [the Enrichment UI prototype](https://github.com/hortonworks/hcp-enrichment_private).

## Development server

Run `npm run start:dev` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).


## Live Parser View
### Test Data
This is a test data what you could use for testing:
```
<167>Jan  5 08:52:35 10.22.8.216 %ASA-7-609001: Built local-host inside:10.22.8.205
<166>Jan  5 08:52:35 10.22.8.216 %ASA-6-302021: Teardown ICMP connection for faddr 10.22.8.74/0(LOCAL\\user.name) gaddr 10.22.8.205/0 laddr 10.22.8.205/0
<167>Jan  5 08:52:35 10.22.8.216 %ASA-7-609002: Teardown local-host inside:10.22.8.205 duration 0:00:00
<142>Jan  5 08:52:35 10.22.8.201 %ASA-6-302014: Teardown TCP connection 488167725 for Outside_VPN:147.111.72.16/26436 to DMZ-Inside:10.22.8.53/443 duration 0:00:00 bytes 9687 TCP FINs
<166>Jan  5 08:52:35 10.22.8.216 %ASA-6-302014: Teardown TCP connection 212805593 for outside:10.22.8.223/59614(LOCAL\\user.name) to inside:10.22.8.78/8102 duration 0:00:07 bytes 3433 TCP FINs (user.name)
<174>Jan  5 14:52:35 10.22.8.212 %ASA-6-302013: Built inbound TCP connection 76245503 for outside:10.22.8.233/54209 (10.22.8.233/54209) to inside:198.111.72.238/443 (198.111.72.238/443) (user.name)
<166>Jan  5 08:52:35 10.22.8.216 %ASA-6-302013: Built inbound TCP connection 212806031 for outside:10.22.8.17/58633 (10.22.8.17/58633)(LOCAL\\user.name) to inside:10.22.8.12/389 (10.22.8.12/389) (user.name)
<142>Jan  5 08:52:35 10.22.8.201 %ASA-6-302014: Teardown TCP connection 488168292 for DMZ-Inside:10.22.8.51/51231 to Inside-Trunk:10.22.8.174/40004 duration 0:00:00 bytes 2103 TCP FINs
<142>Jan  5 08:52:35 10.22.8.201 %ASA-6-106015: Deny TCP (no connection) from 186.111.72.11/80 to 204.111.72.226/45019 flags SYN ACK  on interface Outside_VPN
<166>Jan  5 09:52:35 10.22.8.12 %ASA-6-302014: Teardown TCP connection 17604987 for outside:209.111.72.151/443 to inside:10.22.8.188/64306 duration 0:00:31 bytes 10128 TCP FINs
<166>Jan  5 09:52:35 10.22.8.12 %ASA-6-302014: Teardown TCP connection 17604999 for outside:209.111.72.151/443 to inside:10.22.8.188/64307 duration 0:00:30 bytes 6370 TCP FINs
<142>Jan  5 08:52:35 10.22.8.201 %ASA-6-302014: Teardown TCP connection 488167347 for Outside_VPN:198.111.72.24/2134 to DMZ-Inside:10.22.8.53/443 duration 0:00:01 bytes 9785 TCP FINs
<174>Jan  5 14:52:35 10.22.8.212 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)
<166>Jan  5 08:52:35 10.22.8.216 %ASA-6-302014: Teardown TCP connection 212805993 for outside:10.22.8.89/56917(LOCAL\\user.name) to inside:216.111.72.126/443 duration 0:00:00 bytes 0 TCP FINs (user.name)
<167>Jan  5 08:52:35 10.22.8.216 %ASA-7-710005: UDP request discarded from 10.22.8.223/49192 to outside:224.111.72.252/5355
```
