<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Exam Feedback</title>
    <style>
      #content,#page {
          width: 100%;
          margin: 0;
          float: none;
          }
          /** Setting margins */
          @page { margin: 2cm }
          /* Or: */
          @page :left {
          margin: 1cm;
          }
          @page :right {
          margin: 1cm;
          }
          /* The first page of a print can be manipulated as well */
          @page :first {
          margin: 1cm 2cm;
          }
          body {
          font: 13pt "Times New Roman", Times, serif;
          line-height: 1.3;
          background: #fff !important;
          color: #000;
          }
          h1 {
          font-size: 24pt;
          }
          h2, h3, h4 {
          font-size: 14pt;
          margin-top: 25px;
          }

          header {
              position: fixed;
              top: 0;
            }
          footer {
              position: fixed;
              bottom: 0;
            }

            table {
              width: 100%;
              border-collapse: collapse;
              margin: 25px 0;
            }

            table thead th {
              background-color: #eeeeee;
              color: #000000;
              font-weight: bold;
              border: 1px solid #54585d;
            }

            table tbody td {
              color: #000000;
              border: 1px solid #000000;
            }
            table tbody tr {
              background-color: #f9fafb;
            }
            table tbody tr:nth-child(odd) {
              background-color: #ffffff;
            }

            th, td {
              vertical-align: top;
              text-align: left;
            }
  </style>
</head>
<body class="page">
<header>
    &nbsp;
</header>
<footer>
    &nbsp;
</footer>

<#list students as student>
  <div style="page-break-before:always;">
    <h1>${student.student}</h1>
    <h2>Points: ${student.points} / ${exam_max_points}</h2>

    <#list student.exercises as exercise>
    <div style="page-break-inside: avoid;">
        <h3>Exercise ${exercise.label_number}</h3>
        <table>
          <thead>
            <tr><th style="width: 1cm;">#</th><th>Feedback</th><th style="width:2cm">Points</th><th style="width: 1cm">&nbsp;</th></tr>
          </thead>
          <tbody>
          <#list exercise.sub_exercises as sub_exercise>
            <tr><td>${sub_exercise.sub_exercise}</td><td>${sub_exercise.answer.getFeedback()!}</td><td>${sub_exercise.answer.getPoints()!}</td><td>/ ${sub_exercise.sub_exercise.getPoints()!}</td></tr>
          </#list>
            <tr style="font-weight:bold"><td></td><td style="text-align:right">Total:&nbsp;</td><td>${exercise.sum_points!}</td><td>/ ${exercise.max_points!}</td></tr>
          </tbody>
        </table>
      </div>
    </#list>
  </div>
  <p>${student.student}</p>
  </#list>
</body>
</html>