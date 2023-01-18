insert into tb_configs (address, port, yasno_group)
values ('new_address', null, 1);

insert into tb_telegram_chat_ids (chat_id, config_id)
values (0000, currval('sq_tb_configs'));
