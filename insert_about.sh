sed -i '363a\
                AccordionSection("About", Icons.Default.Info, expanded = expandedSection == "About", onExpandedChange = { expandedSection = if (it) "About" else null }) {\
                    Column(\
                        modifier = Modifier.fillMaxWidth().padding(16.dp),\
                        horizontalAlignment = Alignment.CenterHorizontally,\
                        verticalArrangement = Arrangement.spacedBy(8.dp)\
                    ) {\
                        Text(\
                            text = "Made in ❤️ with Sri Lanka",\
                            fontWeight = FontWeight.Bold,\
                            fontSize = 16.sp,\
                            textAlign = TextAlign.Center,\
                            color = MaterialTheme.colorScheme.primary\
                        )\
                        Text(\
                            text = "Developed by Dinush Lakmal\\nEmail: dinushlakmal01@gmail.com",\
                            fontSize = 14.sp,\
                            textAlign = TextAlign.Center,\
                            color = MaterialTheme.colorScheme.onSurfaceVariant\
                        )\
                    }\
                }\
' app/src/main/java/org/slashboard/ime/settings/SettingsActivity.kt
