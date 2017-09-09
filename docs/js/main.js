const positiveColor = 'rgba(46, 160, 238, 1.0)';
const negativeColor = 'rgba(255, 97, 130, 1.0)';
const neutralColor = 'rgba(230, 230, 230, 1.0)';
const borderColor = 'rgba(0, 0, 0, 0.2)';
const borderWidth = 2;
const positiveTweetsLabel = 'Positive Tweets';
const negativeTweetsLabel = 'Negative Tweets';
const neutralTweetsLabel = 'Neutral Tweets';
const oneHourInMillis = 3600000;
const oneSecondInMillis = 1000;
const numberOfTimelineLabels = 12;
const numberOfTweeetsInTable = 5;
const stackId = "stackId";
const delayedRedrawInterval = 100;

var initialPieChartUpdate = 0;

var database = initializeFirebase();
setUpClassifiedTweetsChart(database);
setNeutralUpClassifiedTweetsChart(database);
setUpTweetsTimelineChart(database);
setUpTweetsTable(database);

function initializeFirebase() {
    var config = {
        apiKey: "AIzaSyAeaHrdRhosNV3LWFJiopBtWLqt3QZ7JBU",
        authDomain: "roma-3f234.firebaseapp.com",
        databaseURL: "https://roma-3f234.firebaseio.com",
        projectId: "roma-3f234",
        storageBucket: "roma-3f234.appspot.com",
        messagingSenderId: "959150545251"
    };
    firebase.initializeApp(config);
    return firebase.database();
}

function hideLoading() {
    document.getElementById("loader").style.display = 'none';
}

function showCharts() {
    document.getElementById("charts").style.display = 'block';
}

function updateLastInitialPieChartUpdate() {
    initialPieChartUpdate = new Date().getTime();
}

function canUpdatePieCharts() {
    return new Date().getTime() - initialPieChartUpdate > oneSecondInMillis;
}

function setUpClassifiedTweetsChart(database) {
    var classifiedTweets = new Chart(document.getElementById("positiveVsNegativeTweets"), {
        type: 'doughnut',
        data: {
            datasets: [{
                label: 'Classified Tweets',
                backgroundColor: [
                    positiveColor,
                    negativeColor
                ],
                borderColor: [
                    borderColor,
                    borderColor
                ],
                borderWidth: borderWidth
            }],
            labels: [positiveTweetsLabel, negativeTweetsLabel]
        },
        options: null
    });
    database.ref('classifiedTweetsStats').on('value', function (snapshot) {
        if (!canUpdatePieCharts()) {
            return;
        }
        hideLoading();
        showCharts();
        var oldData = classifiedTweets.data.datasets[0].data;
        var isFirstUpdate = oldData === undefined || oldData.length === 0;
        var stats = snapshot.val();
        var dataset = classifiedTweets.data.datasets[0];
        if (isFirstUpdate) {
            updateLastInitialPieChartUpdate();
            setTimeout(function () {
                dataset.data.push(stats['numberOfPositiveTweets']);
                dataset.data.push(stats['numberOfNegativeTweets']);
                classifiedTweets.update();
            }, delayedRedrawInterval);
        } else {
            dataset.data.pop();
            dataset.data.pop();
            dataset.data.push(stats['numberOfPositiveTweets']);
            dataset.data.push(stats['numberOfNegativeTweets']);
            classifiedTweets.update(0);
        }
    });
}

function setNeutralUpClassifiedTweetsChart(database) {
    var classifiedAndNeutralTweets = new Chart(document.getElementById("positiveVsNegativeVsNeutralTweets"), {
        type: 'doughnut',
        data: {
            datasets: [{
                label: 'Classified Tweets + Neutral Tweets',

                backgroundColor: [
                    positiveColor,
                    negativeColor,
                    neutralColor
                ],
                borderColor: [
                    borderColor,
                    borderColor,
                    borderColor
                ],
                borderWidth: borderWidth
            }],
            labels: [positiveTweetsLabel, negativeTweetsLabel, neutralTweetsLabel]
        },
        options: null
    });
    database.ref('classifiedTweetsStats').on('value', function (snapshot) {
        var stats = snapshot.val();
        var dataset = classifiedAndNeutralTweets.data.datasets[0];
        var oldData = dataset.data;
        var isFirstUpdate = oldData === undefined || oldData.length === 0;
        if (isFirstUpdate) {
            setTimeout(function () {
                dataset.data.push(stats['numberOfPositiveTweets']);
                dataset.data.push(stats['numberOfNegativeTweets']);
                dataset.data.push(stats['numberOfNeutralTweets']);
                classifiedAndNeutralTweets.update();
            }, delayedRedrawInterval);
        } else {
            dataset.data.pop();
            dataset.data.pop();
            dataset.data.pop();
            dataset.data.push(stats['numberOfPositiveTweets']);
            dataset.data.push(stats['numberOfNegativeTweets']);
            dataset.data.push(stats['numberOfNeutralTweets']);
            classifiedAndNeutralTweets.update(0);
        }
    });
}

function setUpTweetsTimelineChart(database) {
    var stackedBar = new Chart(document.getElementById('positiveVsNegativeTweetsTimeline'), {
        type: 'bar',
        data: {
            datasets: [{
                label: positiveTweetsLabel,
                backgroundColor: positiveColor,
                borderColor: borderColor,
                borderWidth: borderWidth,
                stack: stackId
            }, {
                label: negativeTweetsLabel,
                backgroundColor: negativeColor,
                borderColor: borderColor,
                borderWidth: borderWidth,
                stack: stackId
            }],
            options: {
                scales: {
                    xAxes: [{
                        stacked: true
                    }],
                    yAxes: [{
                        stacked: true
                    }]
                }
            }
        }
    });
    var timelineReference = database.ref('classifiedTweetsTimelineStats').limitToLast(numberOfTimelineLabels);
    timelineReference.once('value', function (snapshot) {
        var values = snapshot.val();
        for (var key in values) {
            var element = values[key];
            updateStackedBarChart(stackedBar, element);
        }
        ;
        timelineReference.on('child_added', function (snapshot) {
            updateStackedBarChart(stackedBar, snapshot.val())
        });
        timelineReference.on('child_changed', function (snapshot) {
            updateStackedBarChart(stackedBar, snapshot.val());
        });
    });


    function updateStackedBarChart(stackedBar, snapshot) {
        var snapshotHour = snapshot.hour;
        var labels = stackedBar.data.labels;
        var positiveTweets = stackedBar.data.datasets[0];
        var negativeTweets = stackedBar.data.datasets[1];
        var labelName = calculateSnapshotLabel(snapshot);
        if (!labels.includes(labelName)) {
            labels.push(labelName);
        }
        if (labels.length === positiveTweets.data.length) {
            positiveTweets.data.pop();
            negativeTweets.data.pop();
        }
        positiveTweets.data.push(snapshot.stats['numberOfPositiveTweets']);
        negativeTweets.data.push(snapshot.stats['numberOfNegativeTweets']);
        stackedBar.update(0);
    }

    function calculateSnapshotLabel(snapshot) {
        var snapshotDate = new Date(snapshot.hour * oneHourInMillis);
        var snapshotStartHour = snapshotDate.getHours();
        var nextHour = (snapshotStartHour + 1) % 24;
        var formattedDate = snapshotDate.toLocaleDateString();
        var hoursRange = snapshotStartHour + ':00' + ' to ' + nextHour + ':00';
        return formattedDate + ' - ' + hoursRange;
    }
}

function setUpTweetsTable(database) {
    database.ref('classifiedTweets').limitToLast(numberOfTweeetsInTable).on('child_added', function (snapshot) {
        var tweet = snapshot.val();
        var table = document.getElementById("tweets");
        var row = table.insertRow();
        row.insertCell().innerHTML = tweet['content'];
        row.insertCell().innerHTML = tweet['sentiment'];
        row.insertCell().innerHTML = tweet['score'];
        if (table.rows.length >= numberOfTweeetsInTable) {
            table.deleteRow(1);
        }
    });
}

