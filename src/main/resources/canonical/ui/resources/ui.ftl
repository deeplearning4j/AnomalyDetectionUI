<!DOCTYPE html>
<html>
<head>
    <style type="text/css">
        html, body {
            width: 100%;
            height: 100%;
        }

        .bgcolor {
            background-color: #F1F1F1;
        }

        .hd {
            background-color: #772953;
            font-size: 18px;
            color: #FFFFFF;
        }

        h1 {
            font-family: Georgia, Times, 'Times New Roman', serif;
            font-size: 28px;
            font-style: bold;
            font-variant: normal;
            font-weight: 500;
            line-height: 26.4px;
        }

        h3 {
            font-family: Georgia, Times, 'Times New Roman', serif;
            font-size: 16px;
            font-style: normal;
            font-variant: normal;
            font-weight: 500;
            line-height: 26.4px;
        }

        table, th, td {
            /*Controls the text size in the table*/
            font-size: 9pt;
        }
    </style>
    <title>DeepStack UI</title>

    <#--<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">-->
    <link rel="stylesheet" href="/assets/jquery-ui.css">
    <script src="//code.jquery.com/jquery-1.10.2.js"></script>
    <script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>
    <script>
        $(function() {
            $( "#tabs" ).tabs();
        });
    </script>
</head>
<body class="bgcolor">

<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">

<script src="//ajax.googleapis.com/ajax/libs/jquery/1/jquery.min.js"></script>
<#--<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">-->
<script src="//code.jquery.com/jquery-1.10.2.js"></script>
<script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/d3/3.5.5/d3.min.js"></script>
<script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
<script src="/assets/dl4j-ui.js"></script>

<script>

    var services = ["Neutron","Nova"];
    var divsForService = ["#neutrondiv","#novadiv"];
    //Store last update times:
    var lastUpdateTime = -1;

    //Interval function to do updates:
    setInterval(function () {

        //Get the last update time

        //If necessary: get the new elements, and render them

//        for (var i = 0; i < services.length; i++) {
//
//            var idx = i;
//
//            $.get("/ui/components/" + services[idx], function (data) {
//                console.log("Got data for idx=" + idx + ", services[idx]=" + services[idx] + ", data=" + data);
//
//                var componentArr = JSON.parse(JSON.stringify(data));
//    //            var mainDiv = $('#outerdiv');
//                var serviceDivElement = $(divsForService[idx]);
//                serviceDivElement.html('');
//    //            var componentArray = Component.getComponent(JSON.stringify(data));
//                for (var i = 0; i < componentArr.length; i++) {
//                    var temp = componentArr[i];
//                    var c = Component.getComponent(JSON.stringify(temp));
//                    c.render(serviceDivElement);
//                }
//            });
//        }


        $.get("/ui/components/Neutron", function (data) {
            var componentArr = JSON.parse(JSON.stringify(data));
            var serviceDivElement = $("#neutrondiv");
            serviceDivElement.html('');
            for (var i = 0; i < componentArr.length; i++) {
                var temp = componentArr[i];
                var c = Component.getComponent(JSON.stringify(temp));
                c.render(serviceDivElement);
            }
        });

        $.get("/ui/components/Nova", function (data) {
            var componentArr = JSON.parse(JSON.stringify(data));
            var serviceDivElement = $("#novadiv");
            serviceDivElement.html('');
            for (var i = 0; i < componentArr.length; i++) {
                var temp = componentArr[i];
                var c = Component.getComponent(JSON.stringify(temp));
                c.render(serviceDivElement);
            }
        });

        $.get("/ui/components/Overview", function (data) {
            var componentArr = JSON.parse(JSON.stringify(data));
            var serviceDivElement = $("#configdiv");
            serviceDivElement.html('');
            for (var i = 0; i < componentArr.length; i++) {
                var temp = componentArr[i];
                var c = Component.getComponent(JSON.stringify(temp));
                c.render(serviceDivElement);
            }
        });

    }, 1000);

</script>


<table style="width: 100%; padding: 5px;" class="hd">
    <tbody>
    <tr>
        <td style="width:181px; height:24px; padding-left:15px; padding-right:0px; padding-top:4px; padding-bottom:4px">
            <a href="/"><img src="/assets/logo-canonical-white.png" border="0"/></a></td>
        <td style="width:143px; height:35px; padding-left:15px; padding-right:15px; padding-top:4px; padding-bottom:4px">
            <a href="/"><img src="/assets/skymind_w.png" border="0"/></a></td>
        <td style="font-size:14pt"> DeepStack UI Demo</td>
    </tr>
    </tbody>
</table>

<div style="width:1400px; margin:0 auto;" id="outerdiv">

</div>


<div id="tabs">
    <ul>
        <li><a href="#tabs-1">Overview</a></li>
        <li><a href="#tabs-2">Neutron</a></li>
        <li><a href="#tabs-3">Nova</a></li>
    </ul>
    <div id="tabs-1" class="bgcolor">
        <div style="width:1400px; height:900px; margin:0 auto;" id="configdiv">
            <p>Loading...</p>
        </div>
    </div>
    <div id="tabs-2" class="bgcolor">
        <div style="width:1400px; height:900px; margin:0 auto;" id="neutrondiv">
            <p>Loading...</p>
        </div>
    </div>
    <div id="tabs-3" class="bgcolor">
        <div style="width:1400px; height:900px; margin:0 auto;" id="novadiv">
            <p>Loading...</p>
        </div>
    </div>
</div>

</body>
</html>