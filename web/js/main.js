const positiveColor = 'rgba(46, 160, 238, 1.0)';
const negativeColor = 'rgba(255, 97, 130, 1.0)';
const neutralColor = 'rgba(230, 230, 230, 1.0)'
const borderColor = 'rgba(0, 0, 0, 0.2)';
const positiveTweetsLegendTag = 'Positive Tweets';
const negativeTweetsLegendTag ='Negative Tweets';

var database = initializeFirebase();
setUpClassifiedTweetsChart(database);
setNeutralUpClassifiedTweetsChart(database);

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
                data: [50, 50],
                backgroundColor: [
                    positiveColor,
                    negativeColor
                ],
                borderColor: [
                    borderColor,
                    borderColor
                ],
                borderWidth: 2
            }],
            labels: [positiveTweetsLegendTag, negativeTweetsLegendTag]
        },
        options: null
    });
    database.ref('classifiedTweetsStats').on('value', function (snapshot) {
        var stats = snapshot.val()
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
                data: [33, 33, 34],
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
                borderWidth: 2
            }],
            labels: ['Positive Tweets', 'Negative Tweets', 'Neutral Tweets']
        },
        options: null
    });
    database.ref('classifiedTweetsStats').on('value', function (snapshot) {
        console.log("Stats changed..let's update the pie chart!");
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
