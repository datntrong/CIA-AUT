# CIA - API

## Config

- Config in **common module** `src/main/java/uet/fit/config/CiaConfig.java`

## APIs

|  Method     | api | output example| Description|
   | -------- | ----------- | ---- |------ |
| GET      | `/cia/branches?pro_file_path=<pro_file_path>`    | ["refs/heads/master","refs/remotes/origin/master"]| get all branches in project|
| GET   |    `/cia/commits?name_branch=<name_branch>&size=<limit_commit>&pro_file_path=<pro_file_path>` |  [{"commitName":"c2afc554720c57433d71cadb9c347776fc1d75c0","author":"Huyen","time":"Sep 9, 2021, 10:02:58 PM","message":"first commit\n"}]  | get all commits in branches. NOTE: branch master is `refs/heads/master` eg: http://localhost:8080/cia/commits?name_branch=refs/heads/master&size=5|
| GET   |    `/cia/diff?commit_a=<commit_1>&commit_b=<commit_2>&pro_file_path=<pro_file_path>` |   | compare two version of project with CIA| 
| GET   |    `/cia/{id}/results` |   | get result of changes code in CIA (recently is file <id_client>.csv)| 