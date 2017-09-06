const positiveColor = 'rgba(46, 160, 238, 1.0)';
const negativeColor = 'rgba(255, 97, 130, 1.0)';
const neutralColor = 'rgba(230, 230, 230, 1.0)';
const borderColor = 'rgba(0, 0, 0, 0.2)';
const borderWidth = 2;
const positiveTweetsLabel = 'Positive Tweets';
const negativeTweetsLabel = 'Negative Tweets';
const neutralTweetsLabel = 'Neutral Tweets';
const oneHourInMillis = 3600000;
const numberOfTimelineLabels = 12;

var database = initializeFirebase();
setUpClassifiedTweetsChart(database);
setNeutralUpClassifiedTweetsChart(database);
setUpTweetsTimelineChart(database);

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
        var stats = snapshot.val();
        var dataset = classifiedTweets.data.datasets[0];
        dataset.data.pop();
        dataset.data.pop();
        dataset.data.push(stats['totalNumberOfPositiveTweets']);
        dataset.data.push(stats['totalNumberOfNegativeTweets']);
        classifiedTweets.update(0);
    });
}

function setNeutralUpClassifiedTweetsChart(database) {
    var classifiedTweets2 = new Chart(document.getElementById("positiveVsNegativeVsNeutralTweets"), {
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
        var dataset = classifiedTweets2.data.datasets[0];
        dataset.data.pop();
        dataset.data.pop();
        dataset.data.pop();
        dataset.data.push(stats['totalNumberOfPositiveTweets']);
        dataset.data.push(stats['totalNumberOfNegativeTweets']);
        dataset.data.push(stats['totalNumberOfNeutralTweets']);
        classifiedTweets2.update(0);
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
                borderWidth: borderWidth
            }, {
                label: negativeTweetsLabel,
                backgroundColor: negativeColor,
                borderColor: borderColor,
                borderWidth: borderWidth
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
    timelineReference.on('child_added', function (snapshot) {
        updateStackedBarChart(stackedBar, snapshot.val())
    });
    timelineReference.on('child_changed', function (snapshot) {
        updateStackedBarChart(stackedBar, snapshot.val());
    });

    function updateStackedBarChart(stackedBar, snapshot) {
        var snapshotHour = snapshot.hour;
        var labels = stackedBar.data.labels;
        var positiveTweets = stackedBar.data.datasets[0];
        var negativeTweets = stackedBar.data.datasets[1];
        var labelName = calculateSnapshotLabel(snapshot);
        if (!labels.includes(labelName)) {
            labels.push(labelName);
        } else {
            positiveTweets.data.pop();
            negativeTweets.data.pop();
        }
        positiveTweets.data.push(snapshot.stats.totalNumberOfPositiveTweets);
        negativeTweets.data.push(snapshot.stats.totalNumberOfNegativeTweets);
        stackedBar.update(0);
    }

    function calculateSnapshotLabel(snapshot) {
        var snapshotDate = new Date(snapshot.hour * oneHourInMillis);
        var snapshotStartHour = snapshotDate.getHours();
        var nextHour = (snapshotStartHour + 1) % 24;
        return snapshotStartHour+":00" + " to " + nextHour+":00";
    }
}
