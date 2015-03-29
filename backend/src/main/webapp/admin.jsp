<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>

<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<%@ page import="com.meiste.greg.ptwgame.entities.Driver" %>
<%@ page import="com.meiste.greg.ptwgame.entities.Race" %>
<%@ page import="com.meiste.greg.ptwgame.entities.RaceCorrectAnswers" %>
<%@ page import="com.meiste.greg.ptwgame.entities.RaceQuestions" %>

<html>
  <head>
    <title>Pick The Winner Administration</title>
    <link href='http://fonts.googleapis.com/css?family=Roboto:400,300' rel='stylesheet' type='text/css'>
    <link href="css/multi-select.css" media="screen" rel="stylesheet" type="text/css">
    <link href="css/admin.css" media="screen" rel="stylesheet" type="text/css">
  </head>
  <body>
    <h1>Pick The Winner Administration</h1>
    
    <% final UserService userService = UserServiceFactory.getUserService(); %>
    <p><a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">Sign out</a></p>
    
<%
    final List<RaceQuestions> qAll = RaceQuestions.getAll();
    RaceQuestions questions = null;
    if (qAll != null) {
        for (final RaceQuestions temp : qAll) {
            if (RaceCorrectAnswers.get(temp.getRace()) == null) {
                questions = temp;
                break;
            }
        }
    }
    if (questions != null) {
        final Race race = questions.getRace();
%>
        <h2>Submit Answers for <%= race.trackNameLong %></h2>
<%
        if (race.isFuture()) {
%>
            <p>Race not finished. Come back later.</p>
<%
        } else {
%>
            <form action="/admin" method="post">
                <input type="hidden" name="op" value="answers">
                <input type="hidden" name="race_id" value="<%= race.id %>">
                <p>Pick The Winner</p>
                <div><select name="a1">
<%
                for (final Driver driver : questions.drivers) {
%>
                    <option value="<%= driver.number %>"><%= driver.getName() %></option>
<%
                }
%>
                </select></div>
                <p><%= questions.q2 %></p>
                <div><select name="a2">
<%
                for (int i = 0; i < questions.a2.length; ++i) {
%>
                    <option value="<%= i %>"><%= questions.a2[i] %></option>
<%
                }
%>
                </select></div>
                <p><%= questions.q3 %></p>
                <div><select name="a3">
<%
                for (int i = 0; i < questions.a3.length; ++i) {
%>
                    <option value="<%= i %>"><%= questions.a3[i] %></option>
<%
                }
%>
                </select></div>
                <p>Which driver will lead the most laps?</p>
                <div><select name="a4">
<%
                for (final Driver driver : questions.drivers) {
%>
                    <option value="<%= driver.number %>"><%= driver.getName() %></option>
<%
                }
%>
                </select></div>
                <p>How many drivers will lead a lap?</p>
                <div><select name="a5">
                    <option value="0">1 - 5 drivers</option>
                    <option value="1">6 - 10 drivers</option>
                    <option value="2">11 - 15 drivers</option>
                    <option value="3">16 - 20 drivers</option>
                    <option value="4">21 - 25 drivers</option>
                    <option value="5">26 - 30 drivers</option>
                    <option value="6">31 - 43 drivers</option>
                </select></div>
                <div style="margin-top:30px"><input type="submit" value="Submit" /></div>
            </form>
<%
        }
    } else {
        final Race race = Race.getNext(false, false);
        if (race != null) {
%>
            <div id="submit_questions"><h2>Submit Questions for <%= race.trackNameLong %></h2>
            <form action="/admin" method="post">
                <input type="hidden" name="op" value="questions">
                
                <div style="margin-left:3px"><select multiple="multiple" id="drivers" name="drivers">
<%
                final List<Driver> drivers = Driver.getAll();
                RaceQuestions prq = null;
                final Race prevRace = Race.getPrev(race);
                if (prevRace != null) {
                    prq = RaceQuestions.get(prevRace);
                }
                for (final Driver d : drivers) {
                    boolean selected = false;
                    if (prq != null && prq.drivers != null && prq.drivers.contains(d)) {
                        selected = true;
                    }
%>
                    <option value="<%= d.id %>"<% if (selected) { %> selected <% } %> >
                        <%= d.getName() %>
                    </option>
<%
                }
%>
                </select>
                <a href="#" id="add_driver">Add New Driver</a></div>
                
                <div style="margin-top:30px"><input name="q2" type="text" size="70" placeholder="Question 2" required="required" />*</div>
                <div style="margin-left:40px"><input name="q2a1" type="text" size="20" placeholder="Answer 1" required="required" />*</div>
                <div style="margin-left:40px"><input name="q2a2" type="text" size="20" placeholder="Answer 2" required="required" />*</div>
                <div style="margin-left:40px"><input name="q2a3" type="text" size="20" placeholder="Answer 3" /></div>
                <div style="margin-left:40px"><input name="q2a4" type="text" size="20" placeholder="Answer 4" /></div>
                <div style="margin-left:40px"><input name="q2a5" type="text" size="20" placeholder="Answer 5" /></div>
                
                <div style="margin-top:30px"><input name="q3" type="text" size="70" placeholder="Question 3" required="required" />*</div>
                <div style="margin-left:40px"><input name="q3a1" type="text" size="20" placeholder="Answer 1" required="required" />*</div>
                <div style="margin-left:40px"><input name="q3a2" type="text" size="20" placeholder="Answer 2" required="required" />*</div>
                <div style="margin-left:40px"><input name="q3a3" type="text" size="20" placeholder="Answer 3" /></div>
                <div style="margin-left:40px"><input name="q3a4" type="text" size="20" placeholder="Answer 4" /></div>
                <div style="margin-left:40px"><input name="q3a5" type="text" size="20" placeholder="Answer 5" /></div>
                
                <div style="margin-top:30px"><input type="submit" value="Submit" /></div>
            </form>
            </div>
            <div id="submit_driver" style="display:none"><h2>Add New Driver</h2>
            <form id="driver_form" action="/admin" method="post">
                <input type="hidden" name="op" value="driver">

                <div>
                    <input name="driver_fname" id="driver_fname" type="text" size="15" placeholder="First Name" autocomplete="off" required="required" />
                    <input name="driver_lname" type="text" size="15" placeholder="Last Name" autocomplete="off" required="required" />
                </div>
                <input name="driver_num" type="number" step="1" max="999" placeholder="Number" autocomplete="off" required="required" />

                <div style="margin-top:30px">
                    <input type="button" value="Cancel" id="cancel_driver" />
                    <input type="submit" value="Add" />
                </div>
            </form>
            </div>
            <script src="js/jquery-1.10.2.min.js" type="text/javascript"></script>
            <script src="js/jquery.multi-select.js" type="text/javascript"></script>
            <script type="text/javascript">
                $('#drivers').multiSelect({
                    selectableHeader: "<div class='custom-header'>Inactive Drivers</div>",
                    selectionHeader: "<div class='custom-header'>Entry List</div>"
                });
                $("#add_driver").click(function() {
                    $("#submit_questions").hide();
                    $("#submit_driver").show();
                    $("#driver_fname").focus();
                });
                $("#cancel_driver").click(function() {
                    $("#driver_form")[0].reset();
                    $("#submit_questions").show();
                    $("#submit_driver").hide();
                });
            </script>
<%
        } else {
%>
            <p>Season is over!</p>
<%
        }
    }
%>
  </body>
</html>