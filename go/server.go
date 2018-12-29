package main

import (
	"net/http"
)

func handler(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(200)
	w.Write([]byte(r.URL.Path))
}

func main() {
	server()
}

func server() {
	http.HandleFunc("/", handler)
	err := http.ListenAndServe(":8003", nil)
	if err != nil {
		panic(err)
	}
}
