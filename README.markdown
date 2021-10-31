## Как работать
- git clone https://github.com/CrissNamon/clojure-game.git
    > Склонировать репозиторйи себе
- git checkout master
    > Перейти на главную ветку master
- git pull
    > Получить последние изменения с главной ветки
- git branch <название_ветки>
    > Создать ветку для своего задания
- git checkout <название_ветки>
   > Переключится на свою ветку

Каждое задание выполняется в своей ветке. После выполнения задания сделать git push -u origin <название_ветки> и создать pull request в master

![Game schema](https://raw.githubusercontent.com/CrissNamon/clojure-game/master/game_schema_base.png)

## Команды для использования групп
- add_group name
- join_group name
- current_group
- say_group message
- list_groups
- leave_group
