
// class Admin {
//     constructor (password, username) {
//         this.password = password;
//         this.username = username
//     }
//     toString() {
//         return this.password + ', ' + this.username;
//     }
// }

// // Firestore data converter
// var adminConverter = {a
//     toFirestore: function(admin) {
//         return {
//             name: admin.password,
//             state: admin.username
//             }
//     },
//     fromFirestore: function(snapshot, options){
//         const data = snapshot.data(options);
//         return new Admin(data.password, data.username)
//     }
// }

// db.collection("admin").doc("mMHl7JEuIdlHQnzSWdlI")
//   .withConverter(adminConverter)
//   .get().then(function(doc) {
//     if (doc.exists){
//       // Convert to City object
//       var city = doc.data();
//       // Use a City instance method
//       console.log(city.toString());
//     } else {
//       console.log("No such document!")
//     }}).catch(function(error) {
//       console.log("Error getting document:", error)
//     });
var db = firebase.firestore();
var auth = firebase.auth();

function login() {
    let emailVal = document.getElementById("emailLogin");
    let passVal = document.getElementById("passwordLogin");

    auth.signInWithEmailAndPassword(emailVal.value, passVal.value).catch(function(error) {
        // Handle Errors here.
        var errorCode = error.code;
        var errorMessage = error.message;
        console.log(errorCode + ": " + errorMessage);
      });

    auth.onAuthStateChanged(function(user) {
    if (user) {
        // User is signed in.
        var email = user.email;

        db.collection("admin").doc(email).get().then(function(doc) {
            if (doc.exists) {
                console.log("Document data:", doc.id);
                self.location="dashboard.html";
                return true;
            } else {
                // doc.data() will be undefined in this case
                console.log("Email " + email + " is not an admin.");
                forgotPassword();
            }
        }).catch(function(error) {
            console.log("Error getting document:", error);
        });
        
    } else {
        return false;
    }
    });
}

function logout() {
    auth.signOut().then(function() {
        // Sign-out successful.
        self.location="index.html";
      }).catch(function(error) {
        
      });
}

