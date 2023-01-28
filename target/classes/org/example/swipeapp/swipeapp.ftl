<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="css/swipeapp.css"/>
</head>
<body>
<div class="app">
  <!--<header class="header"><h2>Swipe App</h2></header>-->
  <div class="content">
    <div class="card-cont">
    <#list students as student>
      <div class="card">
        <div class="card_top white">
          <!--<div class="card_img"><img src="https://www.oth-regensburg.de/fileadmin/user_upload/semesterzeiten-uhr-startseite-261-127_01.png"></div>-->
          <img src="answer_img/${student.student.getId()}/${exercise.getId()}">
          <!--<p class="card_name"></p>-->
        </div>
        <div class="card_btm">
          <p class="card_we">${student.student}</p>
        </div>
        <div class="card_choice m--reject"></div>
        <div class="card_choice m--like"></div>
        <div class="card_drag"></div>
      </div>
      </#list>

    </div>
    <p class="tip">Swipe left (0 P) or right (full points)<br>
    <button>Skip</button>
    </p>
  </div>
</div>
<script type="text/javascript" src="js/jquery-3.6.1.min.js"></script>
<script type="text/javascript" src="js/swipeapp.js" async></script>
</body>
</html>