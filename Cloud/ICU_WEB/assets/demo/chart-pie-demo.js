// Set new default font family and font color to mimic Bootstrap's default styling
Chart.defaults.global.defaultFontFamily = 'Nunito', '-apple-system,system-ui,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif';
Chart.defaults.global.defaultFontColor = '#858796';

// Pie Chart Example
var temp_arr = [];
var pie_var = new XMLHttpRequest();
var pie_obj;
    pie_var.open("GET", "https://hiput6bvql.execute-api.us-east-1.amazonaws.com/icu-api-get-pie-graph");
    pie_var.send();
    pie_var.onreadystatechange = function () {

      if (pie_var.readyState !== XMLHttpRequest.DONE) return;
      if (pie_var.status === 200) {
        pie_obj = JSON.parse(pie_var.responseText);
        
        
        for(const key in pie_obj)
        {
          temp_arr.push(pie_obj[key]);
        }
        
        var ctx = document.getElementById("myPieChart");
        var myPieChart = new Chart(ctx, {
          type: 'doughnut',
          data: {
            labels: ["이어폰", "시계", "주변 사람","손목 위치","책!"],
            datasets: [{
              data: [temp_arr[0], temp_arr[1], temp_arr[2], temp_arr[3], temp_arr[4]],
              backgroundColor: ['#00FF99', '#0099FF', '#00FFCC','#66FFFF','#e5eb76'],
              hoverBackgroundColor: ['#2e59d9', '#17a673', '#2c9faf'],
              hoverBorderColor: "rgba(234, 236, 244, 1)",
            }],
          },
          options: {
            maintainAspectRatio: false,
            tooltips: {
              backgroundColor: "rgb(255,255,255)",
              bodyFontColor: "#858796",
              borderColor: '#dddfeb',
              borderWidth: 1,
              xPadding: 15,
              yPadding: 15,
              displayColors: false,
              caretPadding: 10,
            },
            legend: {
              display: false
            },
            cutoutPercentage: 80,
          },
        });
        

      } else {
        console.log(`[${pie_var.status}] : ${pie_var.statusText}`);
      }
    };
