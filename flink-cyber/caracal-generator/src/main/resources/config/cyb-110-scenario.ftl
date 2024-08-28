<#assign book=utils.randomBook()>  <#-- create variable x -->
{
"dns-stream": {
        "flow_id": ",${utils.randomInt(100000000,500000000)?c}",
        "probe-id": "dpi1",
        "start_ts": "${((ts - 100)/1000)?c}",
        "ip_clt": "${utils.randomIP('192.168.0.0/22')}",
        "ip_srv": "${utils.randomIP()}",
        "port_clt": ",${utils.randomInt(1024, 65535)?c}",
        "port_srv": 53,
        "expiration_ts": "${((ts + 1000)/1000)?c}",
        "path": "base.eth.ip.udp.dns"
    },
"timestamp": "${ts?number_to_datetime?string("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")}",
"remaining": "${book.title()},${book.genre()}"
}