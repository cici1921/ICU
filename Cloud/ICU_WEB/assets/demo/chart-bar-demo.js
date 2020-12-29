// Set new default font family and font color to mimic Bootstrap's default styling
Chart.defaults.global.defaultFontFamily = '-apple-system,system-ui,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif';
Chart.defaults.global.defaultFontColor = '#292b2c';

// Bar Chart Example

var graph_var = new XMLHttpRequest();
    var arr = [];
    var num_arr=[];
    graph_var.open("GET", "https://uvrmnxwp43.execute-api.us-east-1.amazonaws.com/icu-get-cheat-count-for-graph");
    graph_var.send();
    graph_var.onreadystatechange = function () {

      if (graph_var.readyState !== XMLHttpRequest.DONE) return;
      if (graph_var.status === 200) {
        robj = JSON.parse(graph_var.responseText);
        
        robj.forEach((robj) => {
            arr.push(   String(robj.test_num)+"번 응시자"   );    
            num_arr.push(Number(robj.count));  
        });
        var ctx = document.getElementById("myBarChart");

        var myLineChart = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: arr,
            datasets: [{
              label: "Revenue",
              backgroundColor: "rgba(73,231,193,1)",
              borderColor: "rgba(2,117,216,1)",
              data: num_arr,
            }],
          },
          options: {
            scales: {
              xAxes: [{
                time: {
                  unit: 'month'
                },
                gridLines: {
                  display: false
                },
                ticks: {
                  maxTicksLimit: 10
                }
              }],
              yAxes: [{
                ticks: {
                  min: 0,
                  max: 10,
                  maxTicksLimit: 10
                },
                gridLines: {
                  display: true
                }
              }],
            },
            legend: {
              display: false
            }
          }
        });
        

      } else {
        console.log(`[${graph_var.status}] : ${graph_var.statusText}`);
      }
    };

