mock_server_pid=$(ps -ef | grep mockserver_expectations.json | grep -v grep | awk '{print $2}')
if [ -n "$mock_server_pid" ]; then
   echo "killing mock server process " $mock_server_pid
   kill $mock_server_pid
else
   echo "no mock server running"
fi
