Create button has been added and implemented. There is also a refactor to store data inside the database table.

![450034843-5deb9395-c0d5-4b30-8391-8c9e06e889d1](https://github.com/user-attachments/assets/1e72095d-375a-4591-9d66-940314c71b32)

After pulling the git, make sure to perform cds deploy --to sqlite:db/ai2code-database.sqlite to create the database.

Once that is done, you can test out the create button. The name and description can be merely string, but the typeID must be a UUID. Here are several UUIDs that can be used to test it out.

568b091f-25e0-4875-8233-2510024555c2  
61ce2309-19f8-4c84-a783-61365eaef7e3

![450034924-78d54651-fc95-491d-961c-6d79a8761454](https://github.com/user-attachments/assets/07c39f83-a66c-4735-9342-688937b8c7c4)

There will be some changes in the future, such as using custom fragment to get the TaskType instead of relying on manual UUID input.

![450035198-1a7d52be-ad18-4019-9e8e-df068f311f5c](https://github.com/user-attachments/assets/fea5eb5f-2535-4c84-97cf-417b8619de03)
